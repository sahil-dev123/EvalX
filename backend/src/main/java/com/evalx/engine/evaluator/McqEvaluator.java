package com.evalx.engine.evaluator;

import com.evalx.entity.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class McqEvaluator implements QuestionTypeEvaluator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.MCQ;
    }

    @Override
    public boolean evaluate(String candidateAnswer, String correctAnswer) {
        if (candidateAnswer == null || candidateAnswer.isBlank()) {
            log.debug("MCQ: candidateAnswer is blank, returning false");
            return false;
        }
        boolean result = candidateAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
        log.debug("MCQ evaluate: candidate='{}', correct='{}', match={}", candidateAnswer.trim(), correctAnswer.trim(),
                result);
        return result;
    }
}
