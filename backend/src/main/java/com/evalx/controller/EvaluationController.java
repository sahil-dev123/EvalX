package com.evalx.controller;

import com.evalx.constants.ApiConstants;
import com.evalx.constants.LogConstants;
import com.evalx.dto.response.ApiResponse;
import com.evalx.dto.response.EvaluationResponse;
import com.evalx.service.EvaluationService;
import com.evalx.service.ResponseParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiConstants.EVALUATION_API)
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final ResponseParserService responseParserService;

    @PostMapping(ApiConstants.EVALUATE_PATH)
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluateUpload(
            @RequestParam("file") MultipartFile file) {
        log.info(LogConstants.START_METHOD, "evaluateUpload");
        log.info("File upload received: fileName={}, size={} bytes",
                file.getOriginalFilename(), file.getSize());
        // Parse the uploaded file into candidate answers + optional metadata
        ResponseParserService.ResponseData data = responseParserService.parseResponseFile(file);
        EvaluationResponse result = evaluationService.evaluate(data.getAnswers(), data.getMetadata());
        log.info("Evaluation complete: resultId={}, submissionId={}, totalScore={}",
                result.getResultId(), result.getSubmissionId(), result.getTotalScore());
        log.info(LogConstants.END_METHOD, "evaluateUpload");
        return ResponseEntity.ok(ApiResponse.ok("Evaluation complete", result));
    }

    @PostMapping(ApiConstants.EVALUATE_JSON_PATH)
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluateJson(
            @RequestBody Map<String, String> answers) {
        log.info(LogConstants.START_METHOD, "evaluateJson");
        log.debug("JSON evaluation: {} answer entries received", answers.size());
        // Keys are pre-hashed; no metadata provided for manual JSON submissions
        EvaluationResponse result = evaluationService.evaluate(answers, null);
        log.info("Evaluation complete: resultId={}, submissionId={}, totalScore={}",
                result.getResultId(), result.getSubmissionId(), result.getTotalScore());
        log.info(LogConstants.END_METHOD, "evaluateJson");
        return ResponseEntity.ok(ApiResponse.ok("Evaluation complete", result));
    }

    @GetMapping(ApiConstants.RESULTS_PATH)
    public ResponseEntity<ApiResponse<EvaluationResponse>> getResult(@PathVariable Long resultId) {
        log.info(LogConstants.START_PROCESS, "getResult", resultId);
        EvaluationResponse response = evaluationService.getResultById(resultId);
        log.info(LogConstants.COMPLETED_PROCESS, "getResult", resultId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
