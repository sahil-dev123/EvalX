package com.evalx.controller;

import com.evalx.constants.LogConstants;
import com.evalx.dto.request.CreateSectionRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.entity.Section;
import com.evalx.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSection(
            @Valid @RequestBody CreateSectionRequest req) {
        log.info(LogConstants.START_METHOD, "createSection");
        Section s = sectionService.createSection(req);
        log.info(LogConstants.END_METHOD, "createSection");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Section created",
                Map.of("id", s.getId(), "name", s.getName(), "totalQuestions",
                        s.getTotalQuestions() != null ? s.getTotalQuestions() : 0)));
    }

    @GetMapping("/by-shift/{shiftId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSections(@PathVariable Long shiftId) {
        log.info(LogConstants.START_PROCESS, "getSections", shiftId);
        List<Map<String, Object>> sections = sectionService.getSectionsByShiftId(shiftId).stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName(),
                        "totalQuestions", s.getTotalQuestions() != null ? s.getTotalQuestions() : 0,
                        "orderIndex", s.getOrderIndex()))
                .collect(Collectors.toList());
        log.info(LogConstants.DATA_LOADED, sections.size(), "Section");
        return ResponseEntity.ok(ApiResponse.ok(sections));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable Long id) {
        log.info(LogConstants.START_PROCESS, "deleteSection", id);
        sectionService.deleteSection(id);
        log.info(LogConstants.COMPLETED_PROCESS, "deleteSection", id);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted", null));
    }
}
