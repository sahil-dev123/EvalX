package com.evalx.evaluation;

public interface MarkingStrategy {
    boolean evaluateAnswer(String candidateAnswer, String correctAnswer);

    double calculateMarks(boolean isCorrect, double positiveMarks, double negativeMarks);
}
