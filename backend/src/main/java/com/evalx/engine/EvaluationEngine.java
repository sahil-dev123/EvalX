package com.evalx.engine;

import com.evalx.engine.evaluator.QuestionTypeEvaluator;
import com.evalx.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EvaluationEngine {

    private final Map<QuestionType, QuestionTypeEvaluator> evaluatorRegistry;
    private final MarkingPolicyResolver markingPolicyResolver;

    public EvaluationEngine(List<QuestionTypeEvaluator> evaluators, MarkingPolicyResolver resolver) {
        this.evaluatorRegistry = evaluators.stream()
                .collect(Collectors.toMap(QuestionTypeEvaluator::getSupportedType, Function.identity()));
        this.markingPolicyResolver = resolver;
        log.info("EvaluationEngine initialized with evaluators: {}", evaluatorRegistry.keySet());
    }

    /**
     * Evaluates all candidate responses against answer keys.
     * Returns a map of sectionId → Score and a total Score.
     */
    public EvaluationOutcome evaluate(List<Question> questions,
                                       Map<String, String> candidateAnswers,
                                       Map<Long, Section> sectionMap) {
        Score totalScore = new Score();
        Map<Long, Score> sectionScores = new HashMap<>();

        for (Question question : questions) {
            Section section = question.getSection();
            Long sectionId = section.getId();
            sectionScores.putIfAbsent(sectionId, new Score());

            Score sectionScore = sectionScores.get(sectionId);
            MarkingPolicy policy = markingPolicyResolver.resolve(section);

            String candidateAnswer = candidateAnswers.get(question.getQuestionHash());
            AnswerKey answerKey = question.getAnswerKey();

            if (answerKey == null) {
                log.warn("No answer key for question {}", question.getQuestionNumber());
                continue;
            }

            if (candidateAnswer == null || candidateAnswer.isBlank()) {
                // Unattempted
                sectionScore.addSkipped(policy.getUnattemptedMarks());
                totalScore.addSkipped(policy.getUnattemptedMarks());
            } else {
                QuestionTypeEvaluator evaluator = evaluatorRegistry.get(question.getQuestionType());
                if (evaluator == null) {
                    evaluator = evaluatorRegistry.get(QuestionType.MCQ); // Default fallback
                }

                boolean isCorrect = evaluator.evaluate(candidateAnswer, answerKey.getCorrectAnswer());
                if (isCorrect) {
                    sectionScore.addCorrect(policy.getCorrectMarks());
                    totalScore.addCorrect(policy.getCorrectMarks());
                } else {
                    sectionScore.addIncorrect(policy.getNegativeMarks());
                    totalScore.addIncorrect(policy.getNegativeMarks());
                }
            }
        }

        return new EvaluationOutcome(totalScore, sectionScores);
    }

    public record EvaluationOutcome(Score totalScore, Map<Long, Score> sectionScores) {}
}
