package com.evalx.constants;

public final class ApiConstants {
    private ApiConstants() {
    }

    public static final String BASE_API = "/api";
    public static final String EVALUATION_API = BASE_API + "/evaluation";
    public static final String EXAMS_API = BASE_API + "/exams";
    public static final String EXAM_STAGES_API = BASE_API + "/exam-stages";
    public static final String EXAM_YEARS_API = BASE_API + "/exam-years";
    public static final String SHIFTS_API = BASE_API + "/shifts";
    public static final String ADMIN_API = BASE_API + "/admin";
    public static final String AUTH_API = BASE_API + "/auth";

    public static final String EVALUATE_PATH = "/evaluate";
    public static final String EVALUATE_JSON_PATH = "/evaluate/json";
    public static final String RESULTS_PATH = "/results/{resultId}";
}
