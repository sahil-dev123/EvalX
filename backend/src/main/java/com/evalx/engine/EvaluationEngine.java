package com.evalx.engine;

import com.evalx.constants.LogConstants;
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
        log.info(LogConstants.START_METHOD, "evaluate");
        Score totalScore = new Score();
        Map<Long, Score> sectionScores = new HashMap<>();

        log.debug("Evaluating {} questions", questions.size());

        for (Question question : questions) {
            Section section = question.getSection();
            Long sectionId = section.getId();
            sectionScores.putIfAbsent(sectionId, new Score());

            Score sectionScore = sectionScores.get(sectionId);
            MarkingPolicy policy = markingPolicyResolver.resolve(section);

            String candidateAnswer = candidateAnswers.get(question.getQuestionHash());
            AnswerKey answerKey = question.getAnswerKey();

            if (answerKey == null) {
                log.warn("No answer key found for questionId={}", question.getId());
                continue;
            }

            // Using centralized evaluation logic
            evaluateAnswer(question, candidateAnswer, answerKey, policy, sectionScore, totalScore);
        }

        log.info(LogConstants.END_METHOD, "evaluate");
        return new EvaluationOutcome(totalScore, sectionScores);
    }

    private void evaluateAnswer(Question question, String candidateAnswer, AnswerKey answerKey,
            MarkingPolicy policy, Score sectionScore, Score totalScore) {
        if (candidateAnswer == null || candidateAnswer.isBlank()) {
            log.debug("Question {} unattempted", question.getQuestionNumber());
            sectionScore.addSkipped(policy.getUnattemptedMarks());
            totalScore.addSkipped(policy.getUnattemptedMarks());
        } else {
            QuestionTypeEvaluator evaluator = evaluatorRegistry.getOrDefault(question.getQuestionType(),
                    evaluatorRegistry.get(QuestionType.MCQ));

            boolean isCorrect = evaluator.evaluate(candidateAnswer, answerKey.getCorrectAnswer());
            if (isCorrect) {
                log.debug("Question {} CORRECT", question.getQuestionNumber());
                sectionScore.addCorrect(policy.getCorrectMarks());
                totalScore.addCorrect(policy.getCorrectMarks());
            } else {
                log.debug("Question {} INCORRECT. Applying negative marking: {}", question.getQuestionNumber(),
                        policy.getNegativeMarks());
                sectionScore.addIncorrect(policy.getNegativeMarks());
                totalScore.addIncorrect(policy.getNegativeMarks());
            }
        }
    }

    public double calculateMaxScoreForSection(Section section) {
        MarkingPolicy policy = markingPolicyResolver.resolve(section);
        int totalQ = section.getTotalQuestions() != null ? section.getTotalQuestions() : 0;
        return totalQ * policy.getCorrectMarks();
    }

    public record EvaluationOutcome(Score totalScore, Map<Long, Score> sectionScores) {
    }
}
