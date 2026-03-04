package com.evalx.service;

import com.evalx.constants.ExamConstants;
import com.evalx.constants.LogConstants;
import com.evalx.entity.*;
import com.evalx.repository.AnswerKeyRepository;
import com.evalx.repository.QuestionRepository;
import com.evalx.repository.ExamRepository;
import com.evalx.repository.ExamStageRepository;
import com.evalx.repository.ExamYearRepository;
import com.evalx.repository.ShiftRepository;
import com.evalx.repository.MarkingPolicyRepository;
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
    private final ExamManagementService examManagementService;
    private final ExamRepository examRepository;
    private final ExamStageRepository examStageRepository;
    private final ExamYearRepository examYearRepository;
    private final ShiftRepository shiftRepository;
    private final MarkingPolicyRepository markingPolicyRepository;

    /**
     * Universal Ingest: Extracts metadata from the master PDF and auto-creates the
     * shift hierarchy.
     */
    @Transactional
    public void universalIngest(MultipartFile questionPaper, MultipartFile answerKey) throws IOException {
        log.info(LogConstants.START_METHOD, "universalIngest");
        String qpText;
        try (PDDocument doc = Loader.loadPDF(questionPaper.getBytes())) {
            qpText = new PDFTextStripper().getText(doc);
        }

        // 1. Extract Metadata from Header
        String examCode = ExamConstants.GATE;
        int year = ExamConstants.DEFAULT_YEAR;
        String shiftName = ExamConstants.DEFAULT_SHIFT;

        // Regex for detecting common exam codes and years
        Matcher metaMatcher = Pattern.compile("(GATE|SSC|CAT)\\s+(\\d{4})").matcher(qpText);
        if (metaMatcher.find()) {
            examCode = metaMatcher.group(1).toUpperCase();
            year = Integer.parseInt(metaMatcher.group(2));
            log.debug("Found metadata: examCode={}, year={}", examCode, year);
        }

        Matcher shiftMatcher = Pattern.compile("(S1|S2|Shift \\d|Morning|Afternoon)").matcher(qpText);
        if (shiftMatcher.find()) {
            shiftName = "Shift " + shiftMatcher.group(1);
            log.debug("Found shift detail: {}", shiftName);
        }

        // 2. Find or Create Hierarchy
        final String finalExamCode = examCode;
        Exam exam = examRepository.findByCode(examCode)
                .orElseGet(() -> {
                    log.info("Creating new exam entity for code: {}", finalExamCode);
                    return examRepository.save(Exam.builder()
                            .code(finalExamCode)
                            .name(finalExamCode.equalsIgnoreCase(ExamConstants.GATE)
                                    ? "Graduate Aptitude Test in Engineering"
                                    : finalExamCode)
                            .description("Automated Exam created by EvalX Magic Ingest")
                            .build());
                });

        ExamStage stage = examStageRepository.findByExamIdAndName(exam.getId(), ExamConstants.DEFAULT_STAGE)
                .orElseGet(() -> {
                    log.info("Creating new exam stage: {}", ExamConstants.DEFAULT_STAGE);
                    return examStageRepository.save(ExamStage.builder()
                            .exam(exam)
                            .name(ExamConstants.DEFAULT_STAGE)
                            .orderIndex(1)
                            .build());
                });

        final int finalYear = year;
        ExamYear examYear = examYearRepository.findByExamStageIdAndYear(stage.getId(), year)
                .orElseGet(() -> {
                    log.info("Creating new exam year: {}", finalYear);
                    return examYearRepository.save(ExamYear.builder()
                            .examStage(stage)
                            .year(finalYear)
                            .build());
                });

        // Create Default Marking Policy if not exists
        if (markingPolicyRepository.findByExamYearIdAndSectionIsNull(examYear.getId()).isEmpty()) {
            double positive = examCode.equalsIgnoreCase(ExamConstants.GATE) ? ExamConstants.GATE_POSITIVE_MARK
                    : ExamConstants.DEFAULT_POSITIVE_MARK;
            double negative = examCode.equalsIgnoreCase(ExamConstants.GATE) ? ExamConstants.GATE_NEGATIVE_MARK
                    : ExamConstants.DEFAULT_NEGATIVE_MARK;

            log.info("Creating default marking policy: correct={}, negative={}", positive, negative);
            markingPolicyRepository.save(MarkingPolicy.builder()
                    .examYear(examYear)
                    .correctMarks(positive)
                    .negativeMarks(negative)
                    .build());
        }

        final String finalShiftName = shiftName;
        Shift shift = shiftRepository.findByExamYearIdAndName(examYear.getId(), shiftName)
                .orElseGet(() -> {
                    log.info("Creating new shift: {}", finalShiftName);
                    return shiftRepository.save(Shift.builder()
                            .examYear(examYear)
                            .name(finalShiftName)
                            .build());
                });

        // 3. Delegate to original seeding logic
        parseAndSeedShift(shift.getId(), questionPaper, answerKey);
        log.info(LogConstants.END_METHOD, "universalIngest");
    }

    /**
     * Parse master question paper and answer key PDFs and seed a Shift.
     */
    @Transactional
    public void parseAndSeedShift(Long shiftId, MultipartFile questionPaper, MultipartFile answerKey)
            throws IOException {
        log.info(LogConstants.START_PROCESS, "parseAndSeedShift", shiftId);
        Shift shift = examManagementService.findShiftById(shiftId);

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

            // Get or create Section - Optimized with cache
            Section section = sectionCache.computeIfAbsent(akData.sectionName, name -> {
                // Let's use simple repo query for section lookup
                return shift.getSections().stream()
                        .filter(s -> s.getName().equalsIgnoreCase(name))
                        .findFirst()
                        .orElseGet(() -> {
                            log.info("Creating new section: {} for shiftId={}", name, shiftId);
                            // Using direct creation for internal seed
                            Section s = Section.builder()
                                    .shift(shift)
                                    .name(name)
                                    .orderIndex(shift.getSections().size() + 1)
                                    .build();
                            shift.getSections().add(s);
                            return s;
                        });
            });

            log.debug("Processing Q.{} with hash: {}", qNum, qHash);

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
        log.info(LogConstants.COMPLETED_PROCESS, "parseAndSeedShift", shiftId);
    }

    private Map<Integer, AnswerKeyData> parseAnswerKey(MultipartFile file) throws IOException {
        log.info("Starting Answer Key parsing from PDF");
        Map<Integer, AnswerKeyData> map = new LinkedHashMap<>();
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Regex for GATE Answer Key: (Q. No) (Type) (Section) (Key/Range)
            // Example: 1 MCQ GA A
            Pattern p = Pattern.compile("(\\d+)\\s+(MCQ|MSQ|NAT)\\s+([\\w-]+)\\s+(.+)");
            Matcher m = p.matcher(text);

            while (m.find()) {
                int qNum = Integer.parseInt(m.group(1));
                QuestionType type = QuestionType.valueOf(m.group(2).toUpperCase());
                String sectionName = m.group(3);
                String key = m.group(4).trim();
                map.put(qNum, new AnswerKeyData(type, sectionName, key));
            }
        }
        return map;
    }

    private Map<Integer, String> parseQuestionPaper(MultipartFile file) throws IOException {
        log.info("Starting Question Paper parsing from PDF");
        Map<Integer, String> map = new LinkedHashMap<>();
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Split by "Q.[number]" patterns using regex for robustness
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
