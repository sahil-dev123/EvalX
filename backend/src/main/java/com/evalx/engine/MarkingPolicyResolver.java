package com.evalx.engine;

import com.evalx.entity.MarkingPolicy;
import com.evalx.entity.Section;
import com.evalx.repository.MarkingPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
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
                log.debug("Resolving marking policy for sectionId={}, examYearId={}", sectionId, examYearId);

                // Try section-specific policy first, then fall back to exam-year global policy
                MarkingPolicy policy = markingPolicyRepository.findByExamYearIdAndSectionId(examYearId, sectionId)
                                .orElseGet(() -> {
                                        log.debug("No section-specific policy for sectionId={}; falling back to exam-year policy",
                                                        sectionId);
                                        return markingPolicyRepository.findByExamYearIdAndSectionIsNull(examYearId)
                                                        .orElseThrow(() -> {
                                                                log.error("No marking policy found for examYearId={}",
                                                                                examYearId);
                                                                return new RuntimeException(
                                                                                "No marking policy found for exam year "
                                                                                                + examYearId);
                                                        });
                                });

                log.debug("Resolved policy id={}, correct={}, negative={} for sectionId={}",
                                policy.getId(), policy.getCorrectMarks(), policy.getNegativeMarks(), sectionId);
                return policy;
        }
}
