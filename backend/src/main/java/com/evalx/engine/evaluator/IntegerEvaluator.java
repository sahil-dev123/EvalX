package com.evalx.engine.evaluator;

import com.evalx.entity.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IntegerEvaluator implements QuestionTypeEvaluator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.INTEGER;
    }

    @Override
    public boolean evaluate(String candidateAnswer, String correctAnswer) {
        if (candidateAnswer == null || candidateAnswer.isBlank()) {
            log.debug("INTEGER: candidateAnswer is blank, returning false");
            return false;
        }
        try {
            int candidate = Integer.parseInt(candidateAnswer.trim());
            int correct = Integer.parseInt(correctAnswer.trim());
            boolean result = candidate == correct;
            log.debug("INTEGER evaluate: candidate={}, correct={}, match={}", candidate, correct, result);
            return result;
        } catch (NumberFormatException e) {
            log.warn("INTEGER: failed to parse answer values — candidate='{}', correct='{}': {}",
                    candidateAnswer, correctAnswer, e.getMessage());
            return false;
        }
    }
}
