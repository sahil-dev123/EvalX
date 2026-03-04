package com.evalx.repository;

import com.evalx.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByExamYearIdOrderByNameAsc(Long examYearId);

    java.util.Optional<Shift> findByExamYearIdAndName(Long examYearId, String name);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Shift s " +
            "JOIN s.examYear y JOIN y.examStage st JOIN st.exam e " +
            "WHERE e.code = :examCode AND y.year = :year " +
            "AND (:shiftName IS NULL OR s.name LIKE %:shiftName%)")
    java.util.Optional<Shift> findMostLikelyMatchingShift(
            @org.springframework.data.repository.query.Param("examCode") String examCode,
            @org.springframework.data.repository.query.Param("year") int year,
            @org.springframework.data.repository.query.Param("shiftName") String shiftName);
}
