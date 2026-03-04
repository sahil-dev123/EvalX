package com.evalx.engine.evaluator;

import com.evalx.entity.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NatEvaluator implements QuestionTypeEvaluator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.NAT;
    }

    @Override
    public boolean evaluate(String candidateAnswer, String correctAnswer) {
        if (candidateAnswer == null || candidateAnswer.trim().isEmpty()) {
            log.debug("NAT: candidateAnswer is blank, returning false");
            return false;
        }
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            log.warn("NAT: correctAnswer is blank — answer key may be missing");
            return false;
        }

        try {
            double candidate = Double.parseDouble(candidateAnswer.trim());

            // Correct answer format for NAT is typically "min to max", e.g., "2.30 to 2.40"
            // or a single exact match "4"
            if (correctAnswer.contains("to")) {
                String[] parts = correctAnswer.split("(?i)to");
                if (parts.length == 2) {
                    double min = Double.parseDouble(parts[0].trim());
                    double max = Double.parseDouble(parts[1].trim());
                    boolean inRange = candidate >= min && candidate <= max;
                    log.debug("NAT range check: candidate={}, range=[{}, {}], result={}", candidate, min, max, inRange);
                    return inRange;
                }
            } else if (correctAnswer.contains("-") && !correctAnswer.startsWith("-")) {
                // Handle dash-separated range format e.g. "2.30-2.40"
                String[] parts = correctAnswer.split("-");
                if (parts.length == 2) {
                    double min = Double.parseDouble(parts[0].trim());
                    double max = Double.parseDouble(parts[1].trim());
                    boolean inRange = candidate >= min && candidate <= max;
                    log.debug("NAT dash-range check: candidate={}, range=[{}, {}], result={}", candidate, min, max,
                            inRange);
                    return inRange;
                }
            }

            // Fallback: exact match with epsilon tolerance for floating-point comparison
            double correct = Double.parseDouble(correctAnswer.trim());
            boolean exactMatch = Math.abs(candidate - correct) < 0.0001;
            log.debug("NAT exact check: candidate={}, correct={}, match={}", candidate, correct, exactMatch);
            return exactMatch;

        } catch (NumberFormatException e) {
            log.warn("NAT: failed to parse answer values — candidate='{}', correct='{}': {}",
                    candidateAnswer, correctAnswer, e.getMessage());
            return false;
        }
    }
}
