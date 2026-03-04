package com.evalx.repository;

import com.evalx.entity.AnswerKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AnswerKeyRepository extends JpaRepository<AnswerKey, Long> {
    Optional<AnswerKey> findByQuestionId(Long questionId);
}
