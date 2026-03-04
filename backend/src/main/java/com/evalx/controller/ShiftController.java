package com.evalx.controller;

import com.evalx.dto.request.CreateShiftRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.entity.Shift;
import com.evalx.service.ExamManagementService;
import com.evalx.service.QuestionPaperParserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ExamManagementService examManagementService;
    private final QuestionPaperParserService questionPaperParserService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createShift(@Valid @RequestBody CreateShiftRequest req) {
        Shift s = examManagementService.createShift(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Shift created",
                Map.of("id", s.getId(), "name", s.getName())));
    }

    @GetMapping("/by-exam-year/{examYearId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getShifts(@PathVariable Long examYearId) {
        List<Map<String, Object>> shifts = examManagementService.getShiftsByExamYearId(examYearId).stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "name", s.getName(),
                        "shiftDate", s.getShiftDate() != null ? s.getShiftDate().toString() : "",
                        "startTime", s.getStartTime() != null ? s.getStartTime().toString() : "",
                        "endTime", s.getEndTime() != null ? s.getEndTime().toString() : ""))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(shifts));
    }

    @PostMapping("/{shiftId}/ingest")
    public ResponseEntity<ApiResponse<String>> ingestMasterPdfs(
            @PathVariable Long shiftId,
            @RequestParam("questionPaper") MultipartFile questionPaper,
            @RequestParam("answerKey") MultipartFile answerKey) {
        try {
            questionPaperParserService.parseAndSeedShift(shiftId, questionPaper, answerKey);
            return ResponseEntity.ok(ApiResponse.ok("Master PDFs ingested and shift seeded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to ingest master PDFs: " + e.getMessage()));
        }
    }
}
