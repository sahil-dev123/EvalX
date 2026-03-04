package com.evalx.repository;

import com.evalx.entity.ExamStage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamStageRepository extends JpaRepository<ExamStage, Long> {
    List<ExamStage> findByExamIdOrderByOrderIndexAsc(Long examId);
}
