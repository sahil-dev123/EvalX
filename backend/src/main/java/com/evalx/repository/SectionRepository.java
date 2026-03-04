package com.evalx.repository;

import com.evalx.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByShiftIdOrderByOrderIndexAsc(Long shiftId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Section s WHERE s.shift.examYear.id = :examYearId ORDER BY s.orderIndex ASC")
    List<Section> findByExamYearIdOrderByOrderIndexAsc(@org.springframework.data.repository.query.Param("examYearId") Long examYearId);
}
