package com.evalx.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalExams;
    private long totalStages;
    private long totalQuestions;
    private long totalEvaluations;
}
