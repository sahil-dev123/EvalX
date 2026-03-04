package com.evalx.controller;

import com.evalx.dto.response.ApiResponse;
import com.evalx.dto.response.EvaluationResponse;
import com.evalx.service.EvaluationService;
import com.evalx.service.ResponseParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final ResponseParserService responseParserService;

    @PostMapping("/evaluate")
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluateUpload(
            @RequestParam("file") MultipartFile file) {
        com.evalx.service.ResponseParserService.ResponseData data = responseParserService.parseResponseFile(file);
        EvaluationResponse result = evaluationService.evaluate(data.getAnswers(), data.getMetadata());
        return ResponseEntity.ok(ApiResponse.ok("Evaluation complete", result));
    }

    @PostMapping("/evaluate/json")
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluateJson(
            @RequestBody Map<String, String> answers) {
        // Assume keys are already hashes, no metadata provided for manual JSON
        EvaluationResponse result = evaluationService.evaluate(answers, null);
        return ResponseEntity.ok(ApiResponse.ok("Evaluation complete", result));
    }

    @GetMapping("/results/{resultId}")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getResult(@PathVariable Long resultId) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getResultById(resultId)));
    }
}
