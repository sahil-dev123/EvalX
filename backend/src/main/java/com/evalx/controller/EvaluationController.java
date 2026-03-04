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
@RequestMapping("/api")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final ResponseParserService responseParserService;

    @PostMapping("/evaluate")
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluateUpload(
            @RequestParam Long examYearId,
            @RequestParam("file") MultipartFile file) {
        Map<Long, String> answers = responseParserService.parseResponseFile(file);
        EvaluationResponse result = evaluationService.evaluate(examYearId, answers);
        return ResponseEntity.ok(ApiResponse.ok("Evaluation complete", result));
    }

    @PostMapping("/evaluate/json")
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluateJson(
            @RequestParam Long examYearId,
            @RequestBody Map<String, String> answers) {
        Map<Long, String> parsedAnswers = new java.util.LinkedHashMap<>();
        answers.forEach((k, v) -> {
            try { parsedAnswers.put(Long.parseLong(k), v); } catch (NumberFormatException ignored) {}
        });
        EvaluationResponse result = evaluationService.evaluate(examYearId, parsedAnswers);
        return ResponseEntity.ok(ApiResponse.ok("Evaluation complete", result));
    }

    @GetMapping("/results/{resultId}")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getResult(@PathVariable Long resultId) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getResultById(resultId)));
    }
}
