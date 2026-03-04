package com.evalx.engine.evaluator;

import com.evalx.entity.QuestionType;
import org.springframework.stereotype.Component;

@Component
public class McqEvaluator implements QuestionTypeEvaluator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.MCQ;
    }

    @Override
    public boolean evaluate(String candidateAnswer, String correctAnswer) {
        if (candidateAnswer == null || candidateAnswer.isBlank()) {
            return false;
        }
        return candidateAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
    }
}
