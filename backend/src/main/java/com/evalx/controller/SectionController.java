package com.evalx.controller;

import com.evalx.dto.request.CreateSectionRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.entity.Section;
import com.evalx.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSection(@Valid @RequestBody CreateSectionRequest req) {
        Section s = sectionService.createSection(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Section created",
                Map.of("id", s.getId(), "name", s.getName(), "totalQuestions", s.getTotalQuestions() != null ? s.getTotalQuestions() : 0)));
    }

    @GetMapping("/by-shift/{shiftId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSections(@PathVariable Long shiftId) {
        List<Map<String, Object>> sections = sectionService.getSectionsByShiftId(shiftId).stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName(),
                        "totalQuestions", s.getTotalQuestions() != null ? s.getTotalQuestions() : 0,
                        "orderIndex", s.getOrderIndex()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(sections));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable Long id) {
        sectionService.deleteSection(id);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted", null));
    }
}
