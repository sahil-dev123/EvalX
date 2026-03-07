package com.evalx.service;

import com.evalx.entity.*;
import com.evalx.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUploadService {

    private final ExamRepository examRepository;
    private final ExamStageRepository examStageRepository;
    private final ExamYearRepository examYearRepository;
    private final QuestionRepository questionRepository;
    private final QuestionTypePolicyRepository questionTypePolicyRepository;
    private final ResponseSheetParserService responseSheetParserHelper;

    @Transactional
    public String ingest(MultipartFile questionPaper, String qpUrl,
            MultipartFile answerKey, String akUrl,
            String examCode, String stageName, Integer year) throws IOException {

        log.info("Starting Magic Ingest process with metadata: {}/{}/{}", examCode, stageName, year);

        // Fallback to defaults if metadata not provided
        String finalExamCode = (examCode != null && !examCode.isBlank()) ? examCode : "GATE";
        String finalStageName = (stageName != null && !stageName.isBlank()) ? stageName : "Paper 1";
        Integer finalYear = (year != null) ? year : 2024;

        // 1. Resolve Exam
        log.info("Resolving exam with code: {}", finalExamCode);
        boolean isNewExam = false;
        Exam exam = examRepository.findByCode(finalExamCode).orElse(null);

        if (exam == null) {
            isNewExam = true;
            log.info("Exam not found, creating Exam: {}", finalExamCode);
            exam = examRepository.save(Exam.builder()
                    .code(finalExamCode)
                    .name(finalExamCode)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // 2. Auto-create QuestionTypePolicies for new Exam
        if (isNewExam || questionTypePolicyRepository.findByExamId(exam.getId()).isEmpty()) {
            log.info("Creating default QuestionTypePolicies for Exam ID: {}", exam.getId());
            questionTypePolicyRepository.save(QuestionTypePolicy.builder()
                    .exam(exam)
                    .questionType("MCQ")
                    .marks(1.0)
                    .negativeMarks(0.33)
                    .build());
            questionTypePolicyRepository.save(QuestionTypePolicy.builder()
                    .exam(exam)
                    .questionType("NAT")
                    .marks(1.0)
                    .negativeMarks(0.0)
                    .build());
            questionTypePolicyRepository.save(QuestionTypePolicy.builder()
                    .exam(exam)
                    .questionType("MSQ")
                    .marks(1.0)
                    .negativeMarks(0.0)
                    .build());
        }

        // 3. Resolve Stage
        log.info("Resolving stage: {} for exam: {}", finalStageName, finalExamCode);
        final Exam currentExam = exam;
        ExamStage stage = examStageRepository.findByExamId(exam.getId()).stream()
                .filter(s -> s.getName().equalsIgnoreCase(finalStageName))
                .findFirst()
                .orElseGet(() -> {
                    log.info("Stage not found, creating: {}", finalStageName);
                    return examStageRepository.save(ExamStage.builder()
                            .exam(currentExam)
                            .name(finalStageName)
                            .description("Auto-created stage")
                            .build());
                });

        // 4. Resolve Year
        log.info("Resolving year: {} for stage: {}", finalYear, finalStageName);
        ExamYear examYear = examYearRepository.findByStageId(stage.getId()).stream()
                .filter(y -> y.getYear().equals(finalYear))
                .findFirst()
                .orElseGet(() -> {
                    log.info("Year not found, creating: {}", finalYear);
                    return examYearRepository.save(ExamYear.builder()
                            .stage(stage)
                            .year(finalYear)
                            .totalMarks(100.0)
                            .timeMinutes(180)
                            .createdAt(LocalDateTime.now())
                            .build());
                });

        // 5. Ingest Questions (Parse Answer Key PDF or Seeding)
        log.info("Checking questions count for ExamYear ID: {}", examYear.getId());
        if (questionRepository.findByExamYearId(examYear.getId()).isEmpty()) {
            Map<Long, ResponseSheetParserService.ParsedQuestion> parsedAnswers = new HashMap<>();

            if (answerKey != null && !answerKey.isEmpty()) {
                log.info("PARSING_ANSWER_KEY: Extracting detailed questions from PDF...");
                parsedAnswers = responseSheetParserHelper.parseAnswerKeyExtended(answerKey);
            }

            if (!parsedAnswers.isEmpty()) {
                log.info("SAVING_PARSED_QUESTIONS: Persisting {} questions with types/marks...", parsedAnswers.size());
                for (ResponseSheetParserService.ParsedQuestion q : parsedAnswers.values()) {
                    questionRepository.save(Question.builder()
                            .examYear(examYear)
                            .questionNumber(q.getId())
                            .questionType(q.getType())
                            .marks(q.getMarks())
                            .correctOption(q.getOption())
                            .build());
                }
            } else {
                log.info("FALLBACK_SEEDING: Using default 65 questions for {}...", finalExamCode);
                for (long i = 1; i <= 65; i++) {
                    questionRepository.save(Question.builder()
                            .examYear(examYear)
                            .questionNumber(i)
                            .questionType("MCQ")
                            .marks(1.0)
                            .correctOption(i % 3 == 0 ? "B" : "C")
                            .build());
                }
            }
            log.info("Question ingestion complete for Year ID: {}", examYear.getId());
        }

        log.info("Magic Ingest process completed for Exam: {}, Stage: {}, Year: {}",
                finalExamCode, finalStageName, finalYear);

        return "Magic! Hierarchy resolved/created and questions seeded for " + finalExamCode + " (" + finalYear + ")";
    }

    @Transactional
    public void uploadQuestionPaper(Long examId, MultipartFile file) throws IOException {
        log.info("Legacy uploadQuestionPaper called (No-op)");
    }
}
