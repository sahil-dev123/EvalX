package com.evalx.engine.evaluator;

import com.evalx.entity.QuestionType;

public interface QuestionTypeEvaluator {
    QuestionType getSupportedType();

    /**
     * @return true if the candidate's answer is correct
     */
    boolean evaluate(String candidateAnswer, String correctAnswer);
}
