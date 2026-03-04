package com.evalx.controller;

import com.evalx.constants.ApiConstants;
import com.evalx.constants.LogConstants;
import com.evalx.dto.request.CreateMarkingPolicyRequest;
import com.evalx.dto.response.ApiResponse;
import com.evalx.entity.MarkingPolicy;
import com.evalx.service.MarkingPolicyService;
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
@RequestMapping(ApiConstants.MARKING_POLICIES_API)
@RequiredArgsConstructor
public class MarkingPolicyController {

    private final MarkingPolicyService markingPolicyService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPolicy(
            @Valid @RequestBody CreateMarkingPolicyRequest req) {
        log.info(LogConstants.START_METHOD, "createPolicy");
        MarkingPolicy p = markingPolicyService.createPolicy(req);
        log.info("Created marking policy: id={}, correct={}, negative={}", p.getId(), p.getCorrectMarks(),
                p.getNegativeMarks());
        log.info(LogConstants.END_METHOD, "createPolicy");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Marking policy created",
                Map.of("id", p.getId(), "correctMarks", p.getCorrectMarks(),
                        "negativeMarks", p.getNegativeMarks(), "unattemptedMarks", p.getUnattemptedMarks())));
    }

    @GetMapping("/by-exam-year/{examYearId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPolicies(@PathVariable Long examYearId) {
        log.info(LogConstants.START_PROCESS, "getPolicies", examYearId);
        List<Map<String, Object>> policies = markingPolicyService.getPoliciesByExamYearId(examYearId).stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "sectionId", p.getSection() != null ? p.getSection().getId() : "global",
                        "sectionName", p.getSection() != null ? p.getSection().getName() : "All Sections",
                        "correctMarks", p.getCorrectMarks(),
                        "negativeMarks", p.getNegativeMarks(),
                        "unattemptedMarks", p.getUnattemptedMarks()))
                .collect(Collectors.toList());
        log.info(LogConstants.DATA_LOADED, policies.size(), "MarkingPolicy");
        return ResponseEntity.ok(ApiResponse.ok(policies));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long id) {
        log.info(LogConstants.START_PROCESS, "deletePolicy", id);
        markingPolicyService.deletePolicy(id);
        log.info(LogConstants.COMPLETED_PROCESS, "deletePolicy", id);
        return ResponseEntity.ok(ApiResponse.ok("Policy deleted", null));
    }
}
