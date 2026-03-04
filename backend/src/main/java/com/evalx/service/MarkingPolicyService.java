package com.evalx.service;

import com.evalx.constants.LogConstants;
import com.evalx.dto.request.CreateMarkingPolicyRequest;
import com.evalx.entity.ExamYear;
import com.evalx.entity.MarkingPolicy;
import com.evalx.entity.Section;
import com.evalx.repository.MarkingPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkingPolicyService {

    private final MarkingPolicyRepository markingPolicyRepository;
    private final ExamManagementService examManagementService;
    private final SectionService sectionService;

    @Transactional
    public MarkingPolicy createPolicy(CreateMarkingPolicyRequest request) {
        log.info(LogConstants.START_METHOD, "createPolicy");
        ExamYear examYear = examManagementService.findExamYearById(request.getExamYearId());
        Section section = request.getSectionId() != null
                ? sectionService.findSectionById(request.getSectionId())
                : null;

        MarkingPolicy policy = MarkingPolicy.builder()
                .examYear(examYear)
                .section(section)
                .correctMarks(request.getCorrectMarks())
                .negativeMarks(request.getNegativeMarks())
                .unattemptedMarks(request.getUnattemptedMarks() != null ? request.getUnattemptedMarks() : 0.0)
                .build();
        MarkingPolicy saved = markingPolicyRepository.save(policy);
        log.info(LogConstants.END_METHOD, "createPolicy");
        return saved;
    }

    public List<MarkingPolicy> getPoliciesByExamYearId(Long examYearId) {
        log.info(LogConstants.START_PROCESS, "getPoliciesByExamYearId", examYearId);
        List<MarkingPolicy> policies = markingPolicyRepository.findByExamYearId(examYearId);
        log.info(LogConstants.DATA_LOADED, policies.size(), "MarkingPolicy");
        return policies;
    }

    @Transactional
    public void deletePolicy(Long id) {
        log.info(LogConstants.START_PROCESS, "deletePolicy", id);
        markingPolicyRepository.deleteById(id);
        log.info(LogConstants.COMPLETED_PROCESS, "deletePolicy", id);
    }
}
