package com.evalx.engine.evaluator;

import com.evalx.entity.QuestionType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MsqEvaluator implements QuestionTypeEvaluator {

    @Override
    public boolean evaluate(String candidateAnswer, String correctAnswer) {
        if (candidateAnswer == null || candidateAnswer.trim().isEmpty()) {
            return false;
        }
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return false;
        }

        // MSQ answers like "A;B" should match "B;A", "A, B", "A; B", etc.
        Set<String> candidateSet = Arrays.stream(candidateAnswer.split("[,;]"))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        Set<String> correctSet = Arrays.stream(correctAnswer.split("[,;]"))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return candidateSet.equals(correctSet);
    }

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.MSQ;
    }
}
