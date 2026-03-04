package com.evalx.repository;

import com.evalx.entity.MarkingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MarkingPolicyRepository extends JpaRepository<MarkingPolicy, Long> {
    List<MarkingPolicy> findByExamYearId(Long examYearId);
    Optional<MarkingPolicy> findByExamYearIdAndSectionId(Long examYearId, Long sectionId);
    Optional<MarkingPolicy> findByExamYearIdAndSectionIsNull(Long examYearId);
}
