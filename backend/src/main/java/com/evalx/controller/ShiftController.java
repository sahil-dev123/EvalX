package com.evalx.controller;

import com.evalx.constants.ApiConstants;
import com.evalx.constants.LogConstants;
import com.evalx.dto.request.CreateShiftRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.entity.Shift;
import com.evalx.service.ExamManagementService;
import com.evalx.service.QuestionPaperParserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(ApiConstants.SHIFTS_API)
@RequiredArgsConstructor
public class ShiftController {

    private final ExamManagementService examManagementService;
    private final QuestionPaperParserService questionPaperParserService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createShift(@Valid @RequestBody CreateShiftRequest req) {
        log.info(LogConstants.START_METHOD, "createShift");
        Shift s = examManagementService.createShift(req);
        log.info("Created shift: id={}, name={}", s.getId(), s.getName());
        log.info(LogConstants.END_METHOD, "createShift");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Shift created",
                Map.of("id", s.getId(), "name", s.getName())));
    }

    @GetMapping("/by-exam-year/{examYearId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getShifts(@PathVariable Long examYearId) {
        log.info(LogConstants.START_PROCESS, "getShifts", examYearId);
        List<Map<String, Object>> shifts = examManagementService.getShiftsByExamYearId(examYearId).stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "name", s.getName(),
                        "shiftDate", s.getShiftDate() != null ? s.getShiftDate().toString() : "",
                        "startTime", s.getStartTime() != null ? s.getStartTime().toString() : "",
                        "endTime", s.getEndTime() != null ? s.getEndTime().toString() : ""))
                .collect(Collectors.toList());
        log.info(LogConstants.DATA_LOADED, shifts.size(), "Shift");
        return ResponseEntity.ok(ApiResponse.ok(shifts));
    }

    @PostMapping("/{shiftId}/ingest")
    public ResponseEntity<ApiResponse<String>> ingestMasterPdfs(
            @PathVariable Long shiftId,
            @RequestParam("questionPaper") MultipartFile questionPaper,
            @RequestParam("answerKey") MultipartFile answerKey) {
        log.info(LogConstants.START_INGEST, questionPaper.getOriginalFilename(), answerKey.getOriginalFilename());
        log.debug("Ingest target shiftId={}", shiftId);
        try {
            questionPaperParserService.parseAndSeedShift(shiftId, questionPaper, answerKey);
            log.info(LogConstants.COMPLETED_INGEST);
            return ResponseEntity.ok(ApiResponse.ok("Master PDFs ingested and shift seeded successfully"));
        } catch (Exception e) {
            log.error(LogConstants.ERROR_PROCESS, "ingestMasterPdfs", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to ingest master PDFs: " + e.getMessage()));
        }
    }
}
