package com.evalx.service;

import com.evalx.dto.ResultResponse;
import com.evalx.engine.EvaluationEngine;
import com.evalx.entity.Result;
import com.evalx.entity.ExamYear;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

        private final EvaluationEngine evaluationEngine;
        private final ResponseSheetParserService responseSheetParserService;

        @Transactional
        public ResultResponse evaluate(Long examYearId, Long examId, MultipartFile responseSheet) throws IOException {
                log.info("EVAL_SERVICE_START: Starting evaluation. YearID: {}, ExamID: {}", examYearId, examId);

                // 1. Parse response sheet
                log.info("PARSING_SHEET: Extracting student responses from PDF...");
                Map<Long, String> candidateResponses = responseSheetParserService.parseResponseSheet(responseSheet);

                log.info("PARSING_COMPLETE: Extracted {} candidate responses.", candidateResponses.size());

                // 2. Delegate to Engine
                log.info("DELEGATING_TO_ENGINE: Calling EvaluationEngine.evaluate()");
                Result result = evaluationEngine.evaluate(examYearId, examId, candidateResponses);

                // 3. Construct flattened ResultResponse for Frontend
                log.info("CONSTRUCTING_RESPONSE: Mapping Result entity to ResultResponse DTO...");
                ExamYear ey = result.getExamYear();

                ResultResponse response = ResultResponse.builder()
                                .totalScore(result.getScore())
                                .maxScore(ey.getTotalMarks())
                                .correct(result.getCorrectCount())
                                .incorrect(result.getWrongCount())
                                .skipped(result.getUnattemptedCount())
                                .totalQuestions(result.getCorrectCount() + result.getWrongCount()
                                                + result.getUnattemptedCount())
                                .accuracy(result.getAccuracy())
                                .examName(ey.getStage().getExam().getName())
                                .stageName(ey.getStage().getName())
                                .year(ey.getYear())
                                .shiftName("N/A")
                                .percentile(0.0)
                                .estimatedRank(0)
                                .totalCandidates(0)
                                .scoreDistribution(new ArrayList<>())
                                .sectionResults(new ArrayList<>())
                                .build();

                log.info("EVAL_SERVICE_FINISH: Evaluation successfully completed and mapped for Year ID: {}. Final Score: {}",
                                examYearId, response.getTotalScore());

                return response;
        }
}
