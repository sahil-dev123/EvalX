package com.evalx.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ResultResponse {
    // Score Summary
    private Double totalScore;
    private Double maxScore;
    private Integer correct;
    private Integer incorrect;
    private Integer skipped;
    private Integer totalQuestions;
    private Double accuracy;

    // Hierarchy Info
    private String examName;
    private String stageName;
    private Integer year;
    private String shiftName;

    // Rank Prediction (Placeholders for now)
    private Double percentile;
    private Integer estimatedRank;
    private Integer totalCandidates;

    // Analytics (Placeholders for charts)
    private Map<String, Object> analytics;
    private List<Map<String, Object>> scoreDistribution;
    private List<Map<String, Object>> sectionResults;
}
