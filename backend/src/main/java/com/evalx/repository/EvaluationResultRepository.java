package com.evalx.repository;

import com.evalx.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {
    Optional<EvaluationResult> findBySubmissionId(Long submissionId);

    @Query("SELECT er FROM EvaluationResult er WHERE er.submission.examYear.id = :examYearId")
    List<EvaluationResult> findByExamYearId(Long examYearId);

    @Query("SELECT er.totalScore FROM EvaluationResult er WHERE er.submission.examYear.id = :examYearId")
    List<Double> findAllScoresByExamYearId(Long examYearId);
}
