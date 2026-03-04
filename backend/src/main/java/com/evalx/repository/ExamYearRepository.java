package com.evalx.repository;

import com.evalx.entity.ExamYear;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamYearRepository extends JpaRepository<ExamYear, Long> {
    List<ExamYear> findByExamStageIdOrderByYearDesc(Long stageId);
}
