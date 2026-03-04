package com.evalx.service;

import com.evalx.entity.*;
import com.evalx.repository.AnswerKeyRepository;
import com.evalx.repository.QuestionRepository;
import com.evalx.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionPaperParserService {

    private final QuestionRepository questionRepository;
    private final AnswerKeyRepository answerKeyRepository;
    private final SectionService sectionService;
    private final ShiftService shiftService;
    private final com.evalx.repository.ExamRepository examRepository;
    private final com.evalx.repository.ExamStageRepository examStageRepository;
    private final com.evalx.repository.ExamYearRepository examYearRepository;
    private final com.evalx.repository.ShiftRepository shiftRepository;
    private final com.evalx.repository.MarkingPolicyRepository markingPolicyRepository;

    /**
     * Universal Ingest: Extracts metadata from the master PDF and auto-creates the
     * shift hierarchy.
     */
    @Transactional
    public void universalIngest(MultipartFile questionPaper, MultipartFile answerKey) throws IOException {
        String qpText;
        try (PDDocument doc = Loader.loadPDF(questionPaper.getBytes())) {
            qpText = new PDFTextStripper().getText(doc);
        }

        // 1. Extract Metadata from Header
        // Example: "GATE 2026 8th Feb 26 S2"
        String examCode = "GATE";
        int year = 2026;
        String shiftName = "Shift S1";

        Matcher metaMatcher = Pattern.compile("(GATE|SSC|CAT)\\s+(\\d{4})").matcher(qpText);
        if (metaMatcher.find()) {
            examCode = metaMatcher.group(1).toUpperCase();
            year = Integer.parseInt(metaMatcher.group(2));
        }

        Matcher shiftMatcher = Pattern.compile("(S1|S2|Shift \\d|Morning|Afternoon)").matcher(qpText);
        if (shiftMatcher.find()) {
            shiftName = "Shift " + shiftMatcher.group(1);
        }

        // 2. Find or Create Hierarchy
        final String finalExamCode = examCode;
        Exam exam = examRepository.findByCode(examCode)
                .orElseGet(() -> examRepository.save(Exam.builder()
                        .code(finalExamCode)
                        .name(finalExamCode.equalsIgnoreCase("GATE") ? "Graduate Aptitude Test in Engineering"
                                : finalExamCode)
                        .description("Automated Exam created by EvalX Magic Ingest")
                        .build()));

        ExamStage stage = examStageRepository.findByExamIdAndName(exam.getId(), "Tier 1")
                .orElseGet(() -> examStageRepository.save(ExamStage.builder()
                        .exam(exam)
                        .name("Tier 1")
                        .orderIndex(1)
                        .build()));

        final int finalYear = year;
        ExamYear examYear = examYearRepository.findByExamStageIdAndYear(stage.getId(), year)
                .orElseGet(() -> examYearRepository.save(ExamYear.builder()
                        .examStage(stage)
                        .year(finalYear)
                        .build()));

        // Create Default Marking Policy if not exists
        if (markingPolicyRepository.findByExamYearIdAndSectionIsNull(examYear.getId()).isEmpty()) {
            double positive = examCode.equalsIgnoreCase("GATE") ? 2.0 : 1.0;
            double negative = examCode.equalsIgnoreCase("GATE") ? 0.66 : 0.0;

            markingPolicyRepository.save(MarkingPolicy.builder()
                    .examYear(examYear)
                    .correctMarks(positive)
                    .negativeMarks(negative)
                    .build());
        }

        final String finalShiftName = shiftName;
        Shift shift = shiftRepository.findByExamYearIdAndName(examYear.getId(), shiftName)
                .orElseGet(() -> shiftRepository.save(Shift.builder()
                        .examYear(examYear)
                        .name(finalShiftName)
                        .build()));

        // 3. Delegate to original seeding logic
        parseAndSeedShift(shift.getId(), questionPaper, answerKey);
    }

    /**
     * Parse master question paper and answer key PDFs and seed a Shift.
     */
    @Transactional
    public void parseAndSeedShift(Long shiftId, MultipartFile questionPaper, MultipartFile answerKey)
            throws IOException {
        Shift shift = shiftService.findShiftById(shiftId);

        // 1. Parse Answer Key first to get the mapping of Q.No -> Type, Section, Key
        Map<Integer, AnswerKeyData> answerKeyMap = parseAnswerKey(answerKey);
        log.info("Parsed {} answer keys from PDF", answerKeyMap.size());

        // 2. Parse Question Paper to get the mapping of Q.No -> Text
        Map<Integer, String> questionTextMap = parseQuestionPaper(questionPaper);
        log.info("Parsed {} question texts from PDF", questionTextMap.size());

        // 3. Create Sections if they don't exist, and create Questions
        Map<String, Section> sectionCache = new HashMap<>();

        for (Map.Entry<Integer, AnswerKeyData> entry : answerKeyMap.entrySet()) {
            int qNum = entry.getKey();
            AnswerKeyData akData = entry.getValue();
            String qText = questionTextMap.getOrDefault(qNum, "Text not found in PDF for Q." + qNum);
            String qHash = HashUtil.generateHash(qText);

            // Get or create Section
            Section section = sectionCache.computeIfAbsent(akData.sectionName, name -> {
                List<Section> existing = sectionService.getSectionsByShiftId(shiftId);
                return existing.stream()
                        .filter(s -> s.getName().equalsIgnoreCase(name))
                        .findFirst()
                        .orElseGet(() -> {
                            var req = com.evalx.dto.request.CreateSectionRequest.builder()
                                    .shiftId(shiftId)
                                    .name(name)
                                    .orderIndex(existing.size() + 1)
                                    .build();
                            return sectionService.createSection(req);
                        });
            });

            // Create Question
            Question question = Question.builder()
                    .section(section)
                    .questionNumber((long) qNum)
                    .questionText(qText)
                    .questionHash(qHash)
                    .questionType(akData.type)
                    .build();

            question = questionRepository.save(question);

            // Create AnswerKey
            AnswerKey ak = AnswerKey.builder()
                    .question(question)
                    .correctAnswer(akData.correctAnswer)
                    .build();
            answerKeyRepository.save(ak);
            question.setAnswerKey(ak);
        }
    }

    private Map<Integer, AnswerKeyData> parseAnswerKey(MultipartFile file) throws IOException {
        Map<Integer, AnswerKeyData> map = new LinkedHashMap<>();
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Regex for GATE Answer Key: (Q. No) (Type) (Section) (Key/Range)
            // Example: 1 MCQ GA A
            // Example: 25 MSQ CS-2 A;B
            // Example: 30 NAT CS-2 3 to 3
            Pattern p = Pattern.compile("(\\d+)\\s+(MCQ|MSQ|NAT)\\s+([\\w-]+)\\s+(.+)");
            Matcher m = p.matcher(text);

            while (m.find()) {
                int qNum = Integer.parseInt(m.group(1));
                QuestionType type = QuestionType.valueOf(m.group(2).toUpperCase());
                String sectionName = m.group(3);
                String key = m.group(4).trim();

                // Sometimes "Page X of Y" or other headers might match partially if regex is
                // too loose,
                // but this pattern is specific enough for the data rows.
                map.put(qNum, new AnswerKeyData(type, sectionName, key));
            }
        }
        return map;
    }

    private Map<Integer, String> parseQuestionPaper(MultipartFile file) throws IOException {
        Map<Integer, String> map = new LinkedHashMap<>();
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // GATE Question Paper format:
            // Q.1 Expedite, Hasten, Hurry, __________
            // ...
            // (A) Accelerate
            // (B) Retard

            // Split by "Q.[number]" patterns
            String[] parts = text.split("Q\\.(\\d+)");
            // The split will give us index 1 as text after Q.1, index 2 after Q.2, etc.
            // But we need the number too. Let's use Matcher.

            Pattern p = Pattern.compile("Q\\.(\\d+)(.*?)(?=Q\\.\\d+|$)", Pattern.DOTALL);
            Matcher m = p.matcher(text);

            while (m.find()) {
                int qNum = Integer.parseInt(m.group(1));
                String content = m.group(2).trim();
                // Clean up "Organizing Institute..." footers if present in the chunk
                content = content.replaceAll("(?i)Organizing Institute:.*Page \\d+ of \\d+", "").trim();
                map.put(qNum, content);
            }
        }
        return map;
    }

    private static class AnswerKeyData {
        QuestionType type;
        String sectionName;
        String correctAnswer;

        AnswerKeyData(QuestionType type, String sectionName, String correctAnswer) {
            this.type = type;
            this.sectionName = sectionName;
            this.correctAnswer = correctAnswer;
        }
    }
}
