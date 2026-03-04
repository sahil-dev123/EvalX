package com.evalx.controller;

import com.evalx.dto.request.CreateExamYearRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.dto.response.ExamYearResponse;
import com.evalx.service.ExamManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-years")
@RequiredArgsConstructor
public class ExamYearController {

    private final ExamManagementService examManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExamYearResponse>> createExamYear(@Valid @RequestBody CreateExamYearRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Exam year created", examManagementService.createExamYear(req)));
    }

    @GetMapping("/by-stage/{stageId}")
    public ResponseEntity<ApiResponse<List<ExamYearResponse>>> getYearsByStage(@PathVariable Long stageId) {
        return ResponseEntity.ok(ApiResponse.ok(examManagementService.getYearsByStageId(stageId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamYearResponse>> getExamYear(@PathVariable Long id) {
        var ey = examManagementService.findExamYearById(id);
        // Re-use service to get formatted response
        return ResponseEntity.ok(ApiResponse.ok(examManagementService.getYearsByStageId(ey.getExamStage().getId())
                .stream().filter(y -> y.getId().equals(id)).findFirst().orElse(null)));
    }

    @PatchMapping("/{id}/candidates")
    public ResponseEntity<ApiResponse<ExamYearResponse>> updateCandidates(@PathVariable Long id,
            @RequestParam Long totalCandidates) {
        return ResponseEntity.ok(ApiResponse.ok(examManagementService.updateTotalCandidates(id, totalCandidates)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExamYear(@PathVariable Long id) {
        examManagementService.deleteExamYear(id);
        return ResponseEntity.ok(ApiResponse.ok("Exam year deleted", null));
    }
}
