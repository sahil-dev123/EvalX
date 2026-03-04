package com.evalx.constants;

public final class ExamConstants {
    private ExamConstants() {
    }

    // Exam Type Codes
    public static final String GATE = "GATE";
    public static final String SSC = "SSC";
    public static final String CAT = "CAT";

    // Question Type Strings (used when question type is passed as a plain string)
    public static final String QUESTION_TYPE_MCQ = "MCQ";
    public static final String QUESTION_TYPE_MSQ = "MSQ";
    public static final String QUESTION_TYPE_NAT = "NAT";

    // Default Marking Rules
    public static final double DEFAULT_POSITIVE_MARK = 1.0;
    public static final double DEFAULT_NEGATIVE_MARK = 0.0;
    public static final double GATE_POSITIVE_MARK = 2.0;
    public static final double GATE_NEGATIVE_MARK = 0.66;

    // Candidate Answer Status Labels
    public static final String STATUS_ANSWERED = "Answered";
    public static final String STATUS_NOT_ANSWERED = "Not Answered";

    // Default Values for Auto-Generated Hierarchy
    public static final int DEFAULT_YEAR = 2026;
    public static final String DEFAULT_SHIFT = "Shift S1";
    public static final String DEFAULT_STAGE = "Tier 1";
}
