package com.evalx.repository;

import com.evalx.entity.ExamYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamYearRepository extends JpaRepository<ExamYear, Long> {
    List<ExamYear> findByStageId(Long stageId);
}
