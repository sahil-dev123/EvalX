package com.evalx.service;

import com.evalx.constants.ExamConstants;
import com.evalx.constants.LogConstants;
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
        private final ShiftRepository shiftRepository;
        private final ExamManagementService examManagementService;

        @Transactional
        public EvaluationResponse evaluate(Map<String, String> candidateAnswers, Map<String, String> metadata) {
                log.info(LogConstants.START_METHOD, "evaluate");

                if (candidateAnswers == null || candidateAnswers.isEmpty()) {
                        throw new EvaluationException("Response sheet is empty or unreadable.");
                }

                Long shiftId = null;

                // 1. Try auto-detection from metadata (exam code + year + shift name)
                if (metadata != null && metadata.containsKey("exam") && metadata.containsKey("year")) {
                        String examCode = metadata.get("exam");
                        int year = Integer.parseInt(metadata.get("year"));
                        String shiftName = metadata.get("shift");
                        log.info("Attempting auto-detection via metadata: Exam={}, Year={}, Shift={}", examCode, year,
                                        shiftName);

                        Optional<Shift> matchedShift = shiftRepository.findMostLikelyMatchingShift(examCode, year,
                                        shiftName);
                        if (matchedShift.isPresent()) {
                                shiftId = matchedShift.get().getId();
                                log.info("Auto-detected Shift ID: {}", shiftId);
                        }
                }

                // 2. Fallback: Auto-Detect Shift from the provided candidate answer hashes
                if (shiftId == null) {
                        log.info("Falling back to hash-based shift detection");
                        shiftId = questionRepository.findMostLikelyShiftIdByHashes(candidateAnswers.keySet())
                                        .orElseThrow(() -> new EvaluationException(
                                                        "Could not detect the Exam Shift from the provided response sheet."));
                        log.info("Hash-based detection found shiftId={}", shiftId);
                }

                // 3. Load questions with answer keys for the matched shift
                List<Question> questions = questionRepository.findWithAnswerKeyByShiftId(shiftId);
                if (questions.isEmpty()) {
                        throw new EvaluationException("No questions found for the detected shift.");
                }

                ExamYear examYear = questions.get(0).getSection().getShift().getExamYear();
                Long examYearId = examYear.getId();
                log.info("Evaluation proceeding for examYearId={}, questionCount={}", examYearId, questions.size());

                // Build a lookup map: sectionId → Section to avoid repeated traversals
                Map<Long, Section> sectionMap = questions.stream()
                                .map(Question::getSection)
                                .distinct()
                                .collect(Collectors.toMap(Section::getId, s -> s));
                log.debug(LogConstants.DATA_LOADED, sectionMap.size(), "Sections");

                // Run the evaluation engine
                EvaluationEngine.EvaluationOutcome outcome = evaluationEngine.evaluate(questions, candidateAnswers,
                                sectionMap);

                // Save the submission record
                ResponseSubmission submission = ResponseSubmission.builder()
                                .examYear(examYear)
                                .sessionId(UUID.randomUUID().toString())
                                .build();

                // Map each question to a candidate-response record
                List<CandidateResponse> responses = questions.stream()
                                .map(q -> CandidateResponse.builder()
                                                .submission(submission)
                                                .question(q)
                                                .selectedAnswer(candidateAnswers.get(q.getQuestionHash()))
                                                .build())
                                .collect(Collectors.toList());

                submission.setResponses(responses);
                ResponseSubmission savedSubmission = submissionRepository.save(submission);
                log.debug("Saved submission: submissionId={}", savedSubmission.getId());

                // Calculate max score: prefer the stored value, fall back to question count ×
                // default mark
                double maxScore = examYear.getTotalMarks() != null
                                ? examYear.getTotalMarks()
                                : questions.size() * ExamConstants.GATE_POSITIVE_MARK;

                // Persist the evaluation result
                Score totalScore = outcome.totalScore();
                EvaluationResult result = EvaluationResult.builder()
                                .submission(savedSubmission)
                                .totalScore(totalScore.getTotalScore())
                                .maxScore(maxScore)
                                .correct(totalScore.getCorrect())
                                .incorrect(totalScore.getIncorrect())
                                .skipped(totalScore.getSkipped())
                                .build();

                // Build section scores from the engine outcome, ordered by section index
                List<Section> orderedSections = sectionRepository.findByExamYearIdOrderByOrderIndexAsc(examYearId);
                List<SectionScore> sectionScores = new ArrayList<>();
                for (Section section : orderedSections) {
                        Score secScore = outcome.sectionScores().getOrDefault(section.getId(), new Score());
                        sectionScores.add(SectionScore.builder()
                                        .evaluationResult(result)
                                        .section(section)
                                        .score(secScore.getTotalScore())
                                        .maxScore(evaluationEngine.calculateMaxScoreForSection(section))
                                        .correct(secScore.getCorrect())
                                        .incorrect(secScore.getIncorrect())
                                        .skipped(secScore.getSkipped())
                                        .build());
                }
                result.setSectionScores(sectionScores);
                result = resultRepository.save(result);
                log.debug("Saved evaluation result: resultId={}, totalScore={}", result.getId(),
                                result.getTotalScore());

                // Compute and persist ranking analytics (percentile, z-score, estimated rank)
                updateRankingAnalytics(result, examYearId);

                // Update bucket-based score distribution for analytics charts
                updateScoreDistribution(examYearId, totalScore.getTotalScore());

                log.info(LogConstants.END_METHOD, "evaluate");
                return buildResponse(result, examYear, orderedSections, outcome);
        }

        public EvaluationResponse getResultById(Long resultId) {
                log.info(LogConstants.START_PROCESS, "getResultById", resultId);
                EvaluationResult result = resultRepository.findWithAllDetailsById(resultId)
                                .orElseThrow(() -> new EvaluationException("Result not found with id: " + resultId));

                ExamYear examYear = result.getSubmission().getExamYear();
                List<Section> sections = sectionRepository.findByExamYearIdOrderByOrderIndexAsc(examYear.getId());

                EvaluationResponse response = buildResponse(result, examYear, sections, null);
                log.info(LogConstants.COMPLETED_PROCESS, "getResultById", resultId);
                return response;
        }

        private void updateRankingAnalytics(EvaluationResult result, Long examYearId) {
                log.debug("Updating ranking analytics for resultId={}, examYearId={}", result.getId(), examYearId);
                double percentile = rankingService.calculatePercentile(examYearId, result.getTotalScore());
                double zScore = rankingService.calculateZScore(examYearId, result.getTotalScore());
                long totalCandidates = result.getSubmission().getExamYear().getTotalCandidates() != null
                                ? result.getSubmission().getExamYear().getTotalCandidates()
                                : 0;
                long estimatedRank = rankingService.estimateRank(percentile, totalCandidates);

                result.setPercentile(percentile);
                result.setEstimatedRank(estimatedRank);
                result.setZScore(zScore);
                resultRepository.save(result);
                log.debug("Ranking: percentile={}, zScore={}, estimatedRank={}", percentile, zScore, estimatedRank);
        }

        private void updateScoreDistribution(Long examYearId, double score) {
                // Group scores into 10-point buckets (e.g. 40-50, 50-60)
                int bucket = ((int) Math.floor(score / 10)) * 10;
                String bucketKey = bucket + "-" + (bucket + 10);
                log.debug("Updating score distribution bucket: {} for examYearId={}", bucketKey, examYearId);

                ScoreDistribution found = scoreDistributionRepository
                                .findByExamYearIdAndScoreBucket(examYearId, bucketKey)
                                .orElseGet(() -> ScoreDistribution.builder()
                                                .examYear(examManagementService.findExamYearById(examYearId))
                                                .scoreBucket(bucketKey)
                                                .frequency(0L)
                                                .build());

                found.setFrequency(found.getFrequency() + 1);
                scoreDistributionRepository.save(found);
        }

        private EvaluationResponse buildResponse(EvaluationResult result, ExamYear examYear,
                        List<Section> sections, EvaluationEngine.EvaluationOutcome outcome) {

                List<EvaluationResponse.SectionResult> sectionResults;

                // Use fresh outcome scores for new evaluations; map from DB for historical
                // look-ups
                if (outcome != null) {
                        sectionResults = sections.stream()
                                        .map(section -> {
                                                Score secScore = outcome.sectionScores().getOrDefault(section.getId(),
                                                                new Score());
                                                return mapToSectionResult(section, secScore);
                                        })
                                        .collect(Collectors.toList());
                } else {
                        sectionResults = result.getSectionScores().stream()
                                        .map(ss -> mapToSectionResult(ss.getSection(), ss))
                                        .collect(Collectors.toList());
                }

                // Score distribution for histogram chart
                List<ScoreDistribution> dists = scoreDistributionRepository
                                .findByExamYearIdOrderByScoreBucketAsc(examYear.getId());
                int candidateBucketBase = ((int) Math.floor(result.getTotalScore() / 10)) * 10;
                String candidateBucket = candidateBucketBase + "-" + (candidateBucketBase + 10);

                List<EvaluationResponse.ScoreBucket> scoreBuckets = dists.stream()
                                .map(d -> EvaluationResponse.ScoreBucket.builder()
                                                .bucket(d.getScoreBucket())
                                                .frequency(d.getFrequency())
                                                .isCandidateBucket(d.getScoreBucket().equals(candidateBucket))
                                                .build())
                                .collect(Collectors.toList());

                // Aggregate analytics (mean, stdDev, cutoff) for the exam year
                List<Double> allScores = resultRepository.findAllScoresByExamYearId(examYear.getId());
                double mean = rankingService.calculateMean(allScores);
                double stdDev = rankingService.calculateStdDev(allScores, mean);

                Map<String, Double> difficultyAnalysis = sectionResults.stream()
                                .collect(Collectors.toMap(EvaluationResponse.SectionResult::getSectionName,
                                                EvaluationResponse.SectionResult::getAccuracy));

                EvaluationResponse.AnalyticsData analytics = EvaluationResponse.AnalyticsData.builder()
                                .averageScore(Math.round(mean * 100.0) / 100.0)
                                .highestScore(allScores.stream().mapToDouble(d -> d).max().orElse(0))
                                .lowestScore(allScores.stream().mapToDouble(d -> d).min().orElse(0))
                                .standardDeviation(Math.round(stdDev * 100.0) / 100.0)
                                .expectedCutoff(Math.round((mean + 0.5 * stdDev) * 100.0) / 100.0)
                                .difficultyAnalysis(difficultyAnalysis)
                                .build();

                // Resolve shift name from the first response, with a safe fallback
                Shift shift = result.getSubmission().getResponses().isEmpty() ? null
                                : result.getSubmission().getResponses().get(0).getQuestion().getSection().getShift();

                return EvaluationResponse.builder()
                                .resultId(result.getId())
                                .submissionId(result.getSubmission().getId())
                                .examName(examYear.getExamStage().getExam().getName())
                                .stageName(examYear.getExamStage().getName())
                                .year(examYear.getYear())
                                .shiftName(shift != null ? shift.getName() : ExamConstants.DEFAULT_SHIFT)
                                .totalScore(result.getTotalScore())
                                .maxScore(result.getMaxScore())
                                .correct(result.getCorrect())
                                .incorrect(result.getIncorrect())
                                .skipped(result.getSkipped())
                                .totalQuestions(result.getCorrect() + result.getIncorrect() + result.getSkipped())
                                .percentile(result.getPercentile())
                                .estimatedRank(result.getEstimatedRank())
                                .zScore(result.getZScore())
                                .totalCandidates(examYear.getTotalCandidates() != null ? examYear.getTotalCandidates()
                                                : 0)
                                .sectionResults(sectionResults)
                                .scoreDistribution(scoreBuckets)
                                .analytics(analytics)
                                .build();
        }

        /** Map a fresh Score (from engine) to a SectionResult DTO. */
        private EvaluationResponse.SectionResult mapToSectionResult(Section section, Score score) {
                int totalQ = section.getTotalQuestions() != null ? section.getTotalQuestions() : 0;
                int attempted = score.getCorrect() + score.getIncorrect();
                double accuracy = attempted > 0 ? (score.getCorrect() * 100.0 / attempted) : 0;
                // Use the engine to calculate max score so marking policy is respected per
                // section
                double maxScore = evaluationEngine.calculateMaxScoreForSection(section);

                return EvaluationResponse.SectionResult.builder()
                                .sectionId(section.getId())
                                .sectionName(section.getName())
                                .score(score.getTotalScore())
                                .maxScore(maxScore)
                                .correct(score.getCorrect())
                                .incorrect(score.getIncorrect())
                                .skipped(score.getSkipped())
                                .totalQuestions(totalQ)
                                .accuracy(Math.round(accuracy * 10.0) / 10.0)
                                .build();
        }

        /** Map a persisted SectionScore (from DB) to a SectionResult DTO. */
        private EvaluationResponse.SectionResult mapToSectionResult(Section section, SectionScore ss) {
                int attempted = ss.getCorrect() + ss.getIncorrect();
                double accuracy = attempted > 0 ? (ss.getCorrect() * 100.0 / attempted) : 0;

                return EvaluationResponse.SectionResult.builder()
                                .sectionId(section.getId())
                                .sectionName(section.getName())
                                .score(ss.getScore())
                                .maxScore(ss.getMaxScore())
                                .correct(ss.getCorrect())
                                .incorrect(ss.getIncorrect())
                                .skipped(ss.getSkipped())
                                .totalQuestions(ss.getCorrect() + ss.getIncorrect() + ss.getSkipped())
                                .accuracy(Math.round(accuracy * 10.0) / 10.0)
                                .build();
        }
}
