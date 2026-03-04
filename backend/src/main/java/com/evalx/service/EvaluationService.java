package com.evalx.service;

import com.evalx.dto.response.EvaluationResponse;
import com.evalx.engine.EvaluationEngine;
import com.evalx.engine.Score;
import com.evalx.entity.*;
import com.evalx.exception.EvaluationException;
import com.evalx.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationEngine evaluationEngine;
    private final RankingService rankingService;
    private final QuestionRepository questionRepository;
    private final ResponseSubmissionRepository submissionRepository;
    private final EvaluationResultRepository resultRepository;
    private final ScoreDistributionRepository scoreDistributionRepository;
    private final SectionRepository sectionRepository;
    private final ExamYearService examYearService;

    @Transactional
    public EvaluationResponse evaluate(Long examYearId, Map<Long, String> candidateAnswers) {
        ExamYear examYear = examYearService.findExamYearById(examYearId);

        // Fetch questions with answer keys
        List<Question> questions = questionRepository.findWithAnswerKeyByExamYearId(examYearId);
        if (questions.isEmpty()) {
            throw new EvaluationException("No questions found for this exam year");
        }

        // Build section map
        Map<Long, Section> sectionMap = questions.stream()
                .map(Question::getSection)
                .distinct()
                .collect(Collectors.toMap(Section::getId, s -> s));

        // Run evaluation
        EvaluationEngine.EvaluationOutcome outcome = evaluationEngine.evaluate(questions, candidateAnswers, sectionMap);

        // Save submission
        ResponseSubmission submission = ResponseSubmission.builder()
                .examYear(examYear)
                .sessionId(UUID.randomUUID().toString())
                .build();

        List<CandidateResponse> responses = new ArrayList<>();
        for (Question q : questions) {
            String answer = candidateAnswers.get(q.getQuestionNumber());
            responses.add(CandidateResponse.builder()
                    .submission(submission)
                    .question(q)
                    .selectedAnswer(answer)
                    .build());
        }
        submission.setResponses(responses);
        submission = submissionRepository.save(submission);

        // Calculate total max score
        double maxScore = examYear.getTotalMarks() != null ? examYear.getTotalMarks() : questions.size() * 2.0;

        // Save evaluation result
        Score totalScore = outcome.totalScore();
        EvaluationResult result = EvaluationResult.builder()
                .submission(submission)
                .totalScore(totalScore.getTotalScore())
                .maxScore(maxScore)
                .correct(totalScore.getCorrect())
                .incorrect(totalScore.getIncorrect())
                .skipped(totalScore.getSkipped())
                .build();

        // Save section scores
        List<SectionScore> sectionScores = new ArrayList<>();
        List<Section> orderedSections = sectionRepository.findByExamYearIdOrderByOrderIndexAsc(examYearId);
        for (Section section : orderedSections) {
            Score secScore = outcome.sectionScores().getOrDefault(section.getId(), new Score());
            int totalQInSection = section.getTotalQuestions() != null ? section.getTotalQuestions() : 0;
            sectionScores.add(SectionScore.builder()
                    .evaluationResult(result)
                    .section(section)
                    .score(secScore.getTotalScore())
                    .maxScore((double) totalQInSection * 2) // Will be calculated from marking policy
                    .correct(secScore.getCorrect())
                    .incorrect(secScore.getIncorrect())
                    .skipped(secScore.getSkipped())
                    .build());
        }
        result.setSectionScores(sectionScores);
        result = resultRepository.save(result);

        // Calculate ranking
        double percentile = rankingService.calculatePercentile(examYearId, totalScore.getTotalScore());
        double zScore = rankingService.calculateZScore(examYearId, totalScore.getTotalScore());
        long totalCandidates = examYear.getTotalCandidates() != null ? examYear.getTotalCandidates() : 0;
        long estimatedRank = rankingService.estimateRank(percentile, totalCandidates);

        result.setPercentile(percentile);
        result.setEstimatedRank(estimatedRank);
        result.setZScore(zScore);
        result = resultRepository.save(result);

        // Update score distribution
        updateScoreDistribution(examYearId, totalScore.getTotalScore());

        // Build response
        return buildResponse(result, examYear, orderedSections, outcome);
    }

    public EvaluationResponse getResultById(Long resultId) {
        EvaluationResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new EvaluationException("Result not found with id: " + resultId));

        ExamYear examYear = result.getSubmission().getExamYear();
        List<Section> sections = sectionRepository.findByExamYearIdOrderByOrderIndexAsc(examYear.getId());

        return buildResponseFromResult(result, examYear, sections);
    }

    private void updateScoreDistribution(Long examYearId, double score) {
        int bucket = ((int) Math.floor(score / 10)) * 10;
        String bucketKey = bucket + "-" + (bucket + 10);

        List<ScoreDistribution> distributions = scoreDistributionRepository
                .findByExamYearIdOrderByScoreBucketAsc(examYearId);
        ScoreDistribution found = distributions.stream()
                .filter(d -> d.getScoreBucket().equals(bucketKey))
                .findFirst()
                .orElse(null);

        if (found != null) {
            found.setFrequency(found.getFrequency() + 1);
            scoreDistributionRepository.save(found);
        } else {
            ExamYear ey = examYearService.findExamYearById(examYearId);
            scoreDistributionRepository.save(ScoreDistribution.builder()
                    .examYear(ey)
                    .scoreBucket(bucketKey)
                    .frequency(1L)
                    .build());
        }
    }

    private EvaluationResponse buildResponse(EvaluationResult result, ExamYear examYear,
                                              List<Section> sections, EvaluationEngine.EvaluationOutcome outcome) {
        List<EvaluationResponse.SectionResult> sectionResults = new ArrayList<>();
        for (Section section : sections) {
            Score secScore = outcome.sectionScores().getOrDefault(section.getId(), new Score());
            int totalQ = section.getTotalQuestions() != null ? section.getTotalQuestions() : 0;
            int attempted = secScore.getCorrect() + secScore.getIncorrect();
            double accuracy = attempted > 0 ? (secScore.getCorrect() * 100.0 / attempted) : 0;

            sectionResults.add(EvaluationResponse.SectionResult.builder()
                    .sectionId(section.getId())
                    .sectionName(section.getName())
                    .score(secScore.getTotalScore())
                    .maxScore((double) totalQ * 2)
                    .correct(secScore.getCorrect())
                    .incorrect(secScore.getIncorrect())
                    .skipped(secScore.getSkipped())
                    .totalQuestions(totalQ)
                    .accuracy(Math.round(accuracy * 10.0) / 10.0)
                    .build());
        }

        // Score distribution for chart
        List<ScoreDistribution> dists = scoreDistributionRepository
                .findByExamYearIdOrderByScoreBucketAsc(examYear.getId());
        String candidateBucket = ((int) Math.floor(result.getTotalScore() / 10)) * 10 + "-" +
                (((int) Math.floor(result.getTotalScore() / 10)) * 10 + 10);

        List<EvaluationResponse.ScoreBucket> scoreBuckets = dists.stream()
                .map(d -> EvaluationResponse.ScoreBucket.builder()
                        .bucket(d.getScoreBucket())
                        .frequency(d.getFrequency())
                        .isCandidateBucket(d.getScoreBucket().equals(candidateBucket))
                        .build())
                .collect(Collectors.toList());

        // Analytics
        List<Double> allScores = resultRepository.findAllScoresByExamYearId(examYear.getId());
        double mean = rankingService.calculateMean(allScores);
        double stdDev = rankingService.calculateStdDev(allScores, mean);

        Map<String, Double> difficultyAnalysis = new HashMap<>();
        for (EvaluationResponse.SectionResult sr : sectionResults) {
            difficultyAnalysis.put(sr.getSectionName(), sr.getAccuracy());
        }

        EvaluationResponse.AnalyticsData analytics = EvaluationResponse.AnalyticsData.builder()
                .averageScore(Math.round(mean * 100.0) / 100.0)
                .highestScore(allScores.stream().mapToDouble(d -> d).max().orElse(0))
                .lowestScore(allScores.stream().mapToDouble(d -> d).min().orElse(0))
                .standardDeviation(Math.round(stdDev * 100.0) / 100.0)
                .expectedCutoff(Math.round((mean + 0.5 * stdDev) * 100.0) / 100.0)
                .difficultyAnalysis(difficultyAnalysis)
                .build();

        long totalCandidates = examYear.getTotalCandidates() != null ? examYear.getTotalCandidates() : 0;

        return EvaluationResponse.builder()
                .resultId(result.getId())
                .submissionId(result.getSubmission().getId())
                .totalScore(result.getTotalScore())
                .maxScore(result.getMaxScore())
                .correct(result.getCorrect())
                .incorrect(result.getIncorrect())
                .skipped(result.getSkipped())
                .totalQuestions(result.getCorrect() + result.getIncorrect() + result.getSkipped())
                .percentile(result.getPercentile())
                .estimatedRank(result.getEstimatedRank())
                .zScore(result.getZScore())
                .totalCandidates(totalCandidates)
                .sectionResults(sectionResults)
                .scoreDistribution(scoreBuckets)
                .analytics(analytics)
                .build();
    }

    private EvaluationResponse buildResponseFromResult(EvaluationResult result, ExamYear examYear,
                                                        List<Section> sections) {
        List<EvaluationResponse.SectionResult> sectionResults = result.getSectionScores().stream()
                .map(ss -> {
                    int totalQ = ss.getSection().getTotalQuestions() != null ? ss.getSection().getTotalQuestions() : 0;
                    int attempted = ss.getCorrect() + ss.getIncorrect();
                    double accuracy = attempted > 0 ? (ss.getCorrect() * 100.0 / attempted) : 0;
                    return EvaluationResponse.SectionResult.builder()
                            .sectionId(ss.getSection().getId())
                            .sectionName(ss.getSection().getName())
                            .score(ss.getScore())
                            .maxScore(ss.getMaxScore())
                            .correct(ss.getCorrect())
                            .incorrect(ss.getIncorrect())
                            .skipped(ss.getSkipped())
                            .totalQuestions(totalQ)
                            .accuracy(Math.round(accuracy * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());

        List<ScoreDistribution> dists = scoreDistributionRepository
                .findByExamYearIdOrderByScoreBucketAsc(examYear.getId());
        String candidateBucket = ((int) Math.floor(result.getTotalScore() / 10)) * 10 + "-" +
                (((int) Math.floor(result.getTotalScore() / 10)) * 10 + 10);

        List<EvaluationResponse.ScoreBucket> scoreBuckets = dists.stream()
                .map(d -> EvaluationResponse.ScoreBucket.builder()
                        .bucket(d.getScoreBucket())
                        .frequency(d.getFrequency())
                        .isCandidateBucket(d.getScoreBucket().equals(candidateBucket))
                        .build())
                .collect(Collectors.toList());

        List<Double> allScores = resultRepository.findAllScoresByExamYearId(examYear.getId());
        double mean = rankingService.calculateMean(allScores);
        double stdDev = rankingService.calculateStdDev(allScores, mean);

        Map<String, Double> difficultyAnalysis = new HashMap<>();
        for (EvaluationResponse.SectionResult sr : sectionResults) {
            difficultyAnalysis.put(sr.getSectionName(), sr.getAccuracy());
        }

        EvaluationResponse.AnalyticsData analytics = EvaluationResponse.AnalyticsData.builder()
                .averageScore(Math.round(mean * 100.0) / 100.0)
                .highestScore(allScores.stream().mapToDouble(d -> d).max().orElse(0))
                .lowestScore(allScores.stream().mapToDouble(d -> d).min().orElse(0))
                .standardDeviation(Math.round(stdDev * 100.0) / 100.0)
                .expectedCutoff(Math.round((mean + 0.5 * stdDev) * 100.0) / 100.0)
                .difficultyAnalysis(difficultyAnalysis)
                .build();

        long totalCandidates = examYear.getTotalCandidates() != null ? examYear.getTotalCandidates() : 0;

        return EvaluationResponse.builder()
                .resultId(result.getId())
                .submissionId(result.getSubmission().getId())
                .totalScore(result.getTotalScore())
                .maxScore(result.getMaxScore())
                .correct(result.getCorrect())
                .incorrect(result.getIncorrect())
                .skipped(result.getSkipped())
                .totalQuestions(result.getCorrect() + result.getIncorrect() + result.getSkipped())
                .percentile(result.getPercentile())
                .estimatedRank(result.getEstimatedRank())
                .zScore(result.getZScore())
                .totalCandidates(totalCandidates)
                .sectionResults(sectionResults)
                .scoreDistribution(scoreBuckets)
                .analytics(analytics)
                .build();
    }
}
