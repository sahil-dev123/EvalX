package com.evalx.service;

import com.evalx.dto.request.CreateMarkingPolicyRequest;
import com.evalx.entity.ExamYear;
import com.evalx.entity.MarkingPolicy;
import com.evalx.entity.Section;
import com.evalx.repository.MarkingPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarkingPolicyService {

    private final MarkingPolicyRepository markingPolicyRepository;
    private final ExamYearService examYearService;
    private final SectionService sectionService;

    @Transactional
    public MarkingPolicy createPolicy(CreateMarkingPolicyRequest request) {
        ExamYear examYear = examYearService.findExamYearById(request.getExamYearId());
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
        return markingPolicyRepository.save(policy);
    }

    public List<MarkingPolicy> getPoliciesByExamYearId(Long examYearId) {
        return markingPolicyRepository.findByExamYearId(examYearId);
    }

    @Transactional
    public void deletePolicy(Long id) {
        markingPolicyRepository.deleteById(id);
    }
}
