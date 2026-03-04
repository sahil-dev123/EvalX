package com.evalx.controller;

import com.evalx.dto.request.CreateExamStageRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.dto.response.ExamStageResponse;
import com.evalx.service.ExamManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-stages")
@RequiredArgsConstructor
public class ExamStageController {

    private final ExamManagementService examManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamStageResponse>> createStage(@Valid @RequestBody CreateExamStageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Stage created", examManagementService.createStage(req)));
    }

    @GetMapping("/by-exam/{examId}")
    public ResponseEntity<ApiResponse<List<ExamStageResponse>>> getStagesByExam(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.ok(examManagementService.getStagesByExamId(examId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStage(@PathVariable Long id) {
        examManagementService.deleteStage(id);
        return ResponseEntity.ok(ApiResponse.ok("Stage deleted", null));
    }
}
