package com.evalx.repository;

import com.evalx.entity.ExamStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamStageRepository extends JpaRepository<ExamStage, Long> {
    List<ExamStage> findByExamId(Long examId);
}
