package com.evalx.engine.evaluator;

import com.evalx.entity.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MsqEvaluator implements QuestionTypeEvaluator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.MSQ;
    }

    @Override
    public boolean evaluate(String candidateAnswer, String correctAnswer) {
        if (candidateAnswer == null || candidateAnswer.trim().isEmpty()) {
            log.debug("MSQ: candidateAnswer is blank, returning false");
            return false;
        }
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            log.warn("MSQ: correctAnswer is blank — answer key may be missing");
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

        boolean result = candidateSet.equals(correctSet);
        log.debug("MSQ evaluate: candidate={}, correct={}, match={}", candidateSet, correctSet, result);
        return result;
    }
}
