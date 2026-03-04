package com.evalx.engine;

import com.evalx.entity.MarkingPolicy;
import com.evalx.entity.Section;
import com.evalx.repository.MarkingPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkingPolicyResolver {

    private final MarkingPolicyRepository markingPolicyRepository;

    /**
     * Resolves the marking policy for a section.
     * Priority: section-specific → exam-year-level fallback.
     */
    public MarkingPolicy resolve(Section section) {
        Long examYearId = section.getShift().getExamYear().getId();
        Long sectionId = section.getId();

        // Try section-specific policy first
        return markingPolicyRepository.findByExamYearIdAndSectionId(examYearId, sectionId)
                .orElseGet(() ->
                        // Fallback to exam-year-level policy
                        markingPolicyRepository.findByExamYearIdAndSectionIsNull(examYearId)
                                .orElseThrow(() -> new RuntimeException(
                                        "No marking policy found for exam year " + examYearId))
                );
    }
}
