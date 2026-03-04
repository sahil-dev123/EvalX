package com.evalx.repository;

import com.evalx.entity.ResponseSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResponseSubmissionRepository extends JpaRepository<ResponseSubmission, Long> {
    List<ResponseSubmission> findByExamYearId(Long examYearId);
    long countByExamYearId(Long examYearId);
}
