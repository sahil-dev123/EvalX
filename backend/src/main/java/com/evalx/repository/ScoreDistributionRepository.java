package com.evalx.repository;

import com.evalx.entity.ScoreDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScoreDistributionRepository extends JpaRepository<ScoreDistribution, Long> {
    List<ScoreDistribution> findByExamYearIdOrderByScoreBucketAsc(Long examYearId);
}
