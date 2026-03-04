package com.evalx.dto.response;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResponse {
    private Long resultId;
    private Long submissionId;
    private Double totalScore;
    private Double maxScore;
    private Integer correct;
    private Integer incorrect;
    private Integer skipped;
    private Integer totalQuestions;
    private Double percentile;
    private Long estimatedRank;
    private Double zScore;
    private Long totalCandidates;
    private String examName;
    private String stageName;
    private Integer year;
    private String shiftName;
    private List<SectionResult> sectionResults;
    private List<ScoreBucket> scoreDistribution;
    private AnalyticsData analytics;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SectionResult {
        private Long sectionId;
        private String sectionName;
        private Double score;
        private Double maxScore;
        private Integer correct;
        private Integer incorrect;
        private Integer skipped;
        private Integer totalQuestions;
        private Double accuracy;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreBucket {
        private String bucket;
        private Long frequency;
        private Boolean isCandidateBucket;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalyticsData {
        private Double averageScore;
        private Double highestScore;
        private Double lowestScore;
        private Double standardDeviation;
        private Double expectedCutoff;
        private Map<String, Double> difficultyAnalysis;
    }
}
