package com.evalx.engine.evaluator;

import com.evalx.entity.QuestionType;
import org.springframework.stereotype.Component;

@Component
public class IntegerEvaluator implements QuestionTypeEvaluator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.INTEGER;
    }

    @Override
    public boolean evaluate(String candidateAnswer, String correctAnswer) {
        if (candidateAnswer == null || candidateAnswer.isBlank()) {
            return false;
        }
        try {
            int candidate = Integer.parseInt(candidateAnswer.trim());
            int correct = Integer.parseInt(correctAnswer.trim());
            return candidate == correct;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
