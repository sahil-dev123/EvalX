package com.evalx.engine.evaluator;

import com.evalx.entity.QuestionType;
import org.springframework.stereotype.Component;

@Component
public class NatEvaluator implements QuestionTypeEvaluator {

    @Override
    public boolean evaluate(String candidateAnswer, String correctAnswer) {
        if (candidateAnswer == null || candidateAnswer.trim().isEmpty()) {
            return false;
        }
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
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
                    return candidate >= min && candidate <= max;
                }
            } else if (correctAnswer.contains("-") && !correctAnswer.startsWith("-")) {
                // Sometime range could be format 2.30-2.40
                 String[] parts = correctAnswer.split("-");
                 if (parts.length == 2) {
                     double min = Double.parseDouble(parts[0].trim());
                     double max = Double.parseDouble(parts[1].trim());
                     return candidate >= min && candidate <= max;
                 }
            }

            // Fallback for single value "3 to 3"
            double correct = Double.parseDouble(correctAnswer.trim());
            return Math.abs(candidate - correct) < 0.0001; // epsilon for float comparison

        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.NAT;
    }
}
