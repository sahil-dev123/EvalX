package com.evalx.repository;

import com.evalx.entity.QuestionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionMappingRepository extends JpaRepository<QuestionMapping, Long> {
    List<QuestionMapping> findByPaperId(String paperId);
}
