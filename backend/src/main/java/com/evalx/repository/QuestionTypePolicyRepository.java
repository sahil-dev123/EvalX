package com.evalx.repository;

import com.evalx.entity.QuestionTypePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionTypePolicyRepository extends JpaRepository<QuestionTypePolicy, Long> {
    List<QuestionTypePolicy> findByExamId(Long examId);
}
