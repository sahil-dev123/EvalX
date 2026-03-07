package com.evalx.evaluation;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GateMarkingStrategy implements MarkingStrategy {

    @Override
    public boolean evaluateAnswer(String candidateAnswer, String correctAnswer) {
        if (candidateAnswer == null || correctAnswer == null)
            return false;

        candidateAnswer = candidateAnswer.trim();
        correctAnswer = correctAnswer.trim();

        // Handle NAT Range (e.g., "3 to 3" or "-10.5 to -10.5")
        if (correctAnswer.toLowerCase().contains(" to ")) {
            try {
                String[] range = correctAnswer.toLowerCase().split(" to ");
                double min = Double.parseDouble(range[0].trim());
                double max = Double.parseDouble(range[1].trim());
                double value = Double.parseDouble(candidateAnswer);
                return value >= min && value <= max;
            } catch (Exception e) {
                return false;
            }
        }

        // Handle MSQ (e.g., "A,B" candidate vs "A;B" official)
        if (correctAnswer.contains(";") || candidateAnswer.contains(",")) {
            Set<String> candidateSet = Arrays.stream(candidateAnswer.split("[,;]"))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
            Set<String> officialSet = Arrays.stream(correctAnswer.split("[,;]"))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
            return candidateSet.equals(officialSet);
        }

        // Handle MCQ/Basic
        return candidateAnswer.equalsIgnoreCase(correctAnswer);
    }

    @Override
    public double calculateMarks(boolean isCorrect, double positiveMarks, double negativeMarks) {
        return isCorrect ? positiveMarks : -Math.abs(negativeMarks);
    }
}
