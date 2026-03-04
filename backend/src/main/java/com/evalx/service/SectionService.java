package com.evalx.service;

import com.evalx.dto.request.CreateSectionRequest;
import com.evalx.entity.ExamYear;
import com.evalx.entity.Section;
import com.evalx.exception.ResourceNotFoundException;
import com.evalx.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ExamYearService examYearService;

    @Transactional
    public Section createSection(CreateSectionRequest request) {
        ExamYear examYear = examYearService.findExamYearById(request.getExamYearId());
        Section section = Section.builder()
                .examYear(examYear)
                .name(request.getName())
                .totalQuestions(request.getTotalQuestions())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();
        return sectionRepository.save(section);
    }

    public List<Section> getSectionsByExamYearId(Long examYearId) {
        return sectionRepository.findByExamYearIdOrderByOrderIndexAsc(examYearId);
    }

    public Section findSectionById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + id));
    }

    @Transactional
    public void deleteSection(Long id) {
        Section section = findSectionById(id);
        sectionRepository.delete(section);
    }
}
