package com.evalx.repository;

import com.evalx.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByExamYearIdOrderByOrderIndexAsc(Long examYearId);
}
