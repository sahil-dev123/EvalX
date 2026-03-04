package com.evalx.service;

import com.evalx.constants.LogConstants;
import com.evalx.repository.EvaluationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final EvaluationResultRepository evaluationResultRepository;

    /**
     * Calculate percentile using Z-score model.
     * Z = (candidateScore − μ) / σ
     * percentile = Φ(Z) using approximation
     */
    public double calculatePercentile(Long examYearId, double candidateScore) {
        log.info(LogConstants.START_PROCESS, "calculatePercentile", examYearId);
        List<Double> allScores = evaluationResultRepository.findAllScoresByExamYearId(examYearId);
        if (allScores.isEmpty()) {
            log.debug("No scores found for examYearId={}, returning default 50.0", examYearId);
            return 50.0;
        }

        double mean = calculateMean(allScores);
        double stdDev = calculateStdDev(allScores, mean);

        if (stdDev == 0) {
            return candidateScore >= mean ? 99.0 : 50.0;
        }

        double zScore = (candidateScore - mean) / stdDev;
        double percentile = zScoreToPercentile(zScore);
        log.debug("Calculated percentile: {} for score: {}", percentile, candidateScore);
        return percentile;
    }

    /**
     * Estimate rank from percentile.
     * Rank = (1 − percentile/100) × totalCandidates
     */
    public long estimateRank(double percentile, long totalCandidates) {
        if (totalCandidates <= 0)
            return 0;
        long rank = Math.round((1 - percentile / 100.0) * totalCandidates);
        return Math.max(1, rank);
    }

    public double calculateZScore(Long examYearId, double candidateScore) {
        List<Double> allScores = evaluationResultRepository.findAllScoresByExamYearId(examYearId);
        if (allScores.isEmpty())
            return 0;

        double mean = calculateMean(allScores);
        double stdDev = calculateStdDev(allScores, mean);
        if (stdDev == 0)
            return 0;

        return (candidateScore - mean) / stdDev;
    }

    /**
     * Statistics helper methods
     */
    public double calculateMean(List<Double> scores) {
        return scores.stream().mapToDouble(d -> d).average().orElse(0);
    }

    public double calculateStdDev(List<Double> scores, double mean) {
        if (scores.size() < 2)
            return 0;
        double variance = scores.stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .sum() / scores.size();
        return Math.sqrt(variance);
    }

    /**
     * Approximate Φ(z) using Abramowitz & Stegun formula.
     */
    private double zScoreToPercentile(double z) {
        if (z < -6)
            return 0.01;
        if (z > 6)
            return 99.99;

        double sign = z >= 0 ? 1 : -1;
        z = Math.abs(z);

        double t = 1.0 / (1.0 + 0.2316419 * z);
        double d = 0.3989422804014327;
        double pHi = d * Math.exp(-z * z / 2.0) *
                (t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.8212560 + t * 1.3302744)))));

        double percentile;
        if (sign > 0) {
            percentile = (1.0 - pHi) * 100;
        } else {
            percentile = pHi * 100;
        }
        return Math.round(percentile * 100.0) / 100.0;
    }
}
