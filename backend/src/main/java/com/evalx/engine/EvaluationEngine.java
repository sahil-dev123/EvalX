package com.evalx.engine;

import com.evalx.entity.*;
import com.evalx.evaluation.MarkingStrategy;
import com.evalx.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationEngine {

    private final QuestionRepository questionRepository;
    private final QuestionTypePolicyRepository policyRepository;
    private final ResultRepository resultRepository;
    private final ExamYearRepository examYearRepository;
    private final Map<String, MarkingStrategy> strategyMap;

    public Result evaluate(Long examYearId, Long examId, Map<Long, String> candidateResponses) {
        log.info("EVAL_ENGINE_START: Processing evaluation. YearID: {}, ExamID: {}", examYearId, examId);

        final ExamYear resolvedExamYear;
        if (examYearId != null) {
            resolvedExamYear = examYearRepository.findById(examYearId)
                    .orElseThrow(() -> new RuntimeException("CRITICAL: ExamYear not found for ID: " + examYearId));
        } else if (examId != null) {
            log.info("RESOLVING_BY_EXAM_HINT: Attempting to find latest year for Exam ID: {}", examId);
            resolvedExamYear = examYearRepository.findAll().stream()
                    .filter(y -> y.getStage().getExam().getId().equals(examId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No ExamYear found for Exam ID: " + examId));
        } else {
            log.info("AUTO_DETECT_FALLBACK: No IDs provided, using most recent ExamYear in system.");
            resolvedExamYear = examYearRepository.findAll().stream()
                    .sorted((a, b) -> b.getId().compareTo(a.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No data found in system. Please ingest an exam first."));
        }

        Long resolvedExamId = resolvedExamYear.getStage().getExam().getId();
        Long resolvedExamYearId = resolvedExamYear.getId();
        log.info("YEAR_RESOLVED: Year {} (Stage: {}) associated with Exam ID: {}",
                resolvedExamYear.getYear(), resolvedExamYear.getStage().getName(), resolvedExamId);

        log.info("LOADING_QUESTIONS: Fetching questions from DB for year...");
        List<Question> questions = questionRepository.findByExamYearId(resolvedExamYearId);
        log.info("QUESTIONS_LOADED: Found {} questions for this year.", questions.size());

        log.info("LOADING_POLICIES: Fetching marking rules for exam ID {}...", resolvedExamId);
        List<QuestionTypePolicy> policies = policyRepository.findByExamId(resolvedExamId);
        Map<String, QuestionTypePolicy> policyMap = policies.stream()
                .collect(Collectors.toMap(QuestionTypePolicy::getQuestionType, p -> p));
        log.info("POLICIES_MAPPED: Successfully mapped {} question type rules.", policyMap.size());

        MarkingStrategy strategy = strategyMap.get("gateMarkingStrategy");
        log.info("STRATEGY_ACQUIRED: Using system marking strategy: {}", strategy.getClass().getSimpleName());

        double totalScore = 0;
        int correct = 0;
        int wrong = 0;
        int unattempted = 0;

        log.info("CALCULATION_START: Scoring responses for {} questions...", questions.size());
        for (Question q : questions) {
            String candidateAnswer = candidateResponses.get(q.getQuestionNumber());
            QuestionTypePolicy policy = policyMap.get(q.getQuestionType());

            if (candidateAnswer == null || candidateAnswer.isEmpty()
                    || candidateAnswer.equalsIgnoreCase("NOT_ATTEMPTED")) {
                unattempted++;
                continue;
            }

            if (policy == null) {
                log.warn("POLICY_MISSING: No marking policy found for type: {}. Checking for generic MCQ fallback...",
                        q.getQuestionType());
                policy = policyMap.get("MCQ"); // Fallback to generic MCQ
            }

            if (policy == null) {
                log.error(
                        "CRITICAL_POLICY_MISSING: No valid policy found (including fallback) for type: {}. Skipping Q.{}",
                        q.getQuestionType(), q.getQuestionNumber());
                continue;
            }

            // Map numeric choices (1, 2, 3, 4) to (A, B, C, D) for MCQ/MSQ
            String processedCandidate = candidateAnswer;
            if (q.getQuestionType() != null
                    && (q.getQuestionType().equalsIgnoreCase("MCQ") || q.getQuestionType().equalsIgnoreCase("MSQ"))) {
                processedCandidate = mapOptionToLetter(candidateAnswer);
            }

            double positiveMarks = (q.getMarks() != null) ? q.getMarks() : policy.getMarks();
            double negativeMarks = (q.getMarks() != null && q.getMarks() > 1.0) ? (positiveMarks * 2.0 / 3.0)
                    : policy.getNegativeMarks();

            boolean isCorrect = strategy.evaluateAnswer(processedCandidate, q.getCorrectOption());

            if (!isCorrect) {
                log.info("MATCH_FAILURE: Q.{} (ID: {}) | Expected: [{}] | Found: [{}] | Type: {}",
                        q.getId(), q.getQuestionNumber(), q.getCorrectOption(), processedCandidate,
                        q.getQuestionType());
            }

            double marks = strategy.calculateMarks(isCorrect, positiveMarks, negativeMarks);

            totalScore += marks;
            if (isCorrect)
                correct++;
            else
                wrong++;
        }

        // Round totalScore to 2 decimal places to handle floating point issues
        totalScore = Math.round(totalScore * 100.0) / 100.0;

        log.info("CALCULATION_FINISH: Evaluation successfully completed for Year ID: {}. Score: {}", resolvedExamYearId,
                totalScore);

        Result result = Result.builder()
                .examYear(resolvedExamYear)
                .score(totalScore)
                .correctCount(correct)
                .wrongCount(wrong)
                .unattemptedCount(unattempted)
                .accuracy((questions.isEmpty() || (correct + wrong) == 0) ? 0.0
                        : (double) Math.round((double) correct / (correct + wrong) * 10000.0) / 100.0)
                .build();

        log.info("RESULT_PERSISTENCE: Storing result in database...");
        Result savedResult = resultRepository.save(result);
        log.info("ENGINE_COMPLETE: Result stored with ID: {}. Finished Evaluation.", savedResult.getId());

        return savedResult;
    }

    private String mapOptionToLetter(String option) {
        if (option == null)
            return null;
        return switch (option) {
            case "1" -> "A";
            case "2" -> "B";
            case "3" -> "C";
            case "4" -> "D";
            default -> option;
        };
    }
}
