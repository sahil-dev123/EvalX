package com.evalx.constants;

public final class LogConstants {
    private LogConstants() {
    }

    public static final String START_METHOD = "Starting method: {}";
    public static final String END_METHOD = "Completed method: {}";
    public static final String START_PROCESS = "Starting process: {} for id={}";
    public static final String COMPLETED_PROCESS = "Completed process: {} for id={}";
    public static final String ERROR_PROCESS = "Error occurred during {}: {}";
    public static final String DATA_LOADED = "Loaded {} {} record(s)";
    public static final String START_INGEST = "Starting universal ingest for question paper: {} and answer key: {}";
    public static final String COMPLETED_INGEST = "Successfully completed universal ingest";
}
