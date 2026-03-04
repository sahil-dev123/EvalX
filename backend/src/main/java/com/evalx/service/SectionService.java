package com.evalx.service;

import com.evalx.constants.LogConstants;
import com.evalx.dto.request.CreateSectionRequest;
import com.evalx.entity.Shift;
import com.evalx.entity.Section;
import com.evalx.exception.ResourceNotFoundException;
import com.evalx.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ExamManagementService examManagementService;

    @Transactional
    public Section createSection(CreateSectionRequest request) {
        log.info(LogConstants.START_METHOD, "createSection");
        Shift shift = examManagementService.findShiftById(request.getShiftId());
        Section section = Section.builder()
                .shift(shift)
                .name(request.getName())
                .totalQuestions(request.getTotalQuestions())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();
        Section saved = sectionRepository.save(section);
        log.info(LogConstants.END_METHOD, "createSection");
        return saved;
    }

    public List<Section> getSectionsByShiftId(Long shiftId) {
        log.info(LogConstants.START_PROCESS, "getSectionsByShiftId", shiftId);
        List<Section> sections = sectionRepository.findByShiftIdOrderByOrderIndexAsc(shiftId);
        log.info(LogConstants.DATA_LOADED, sections.size(), "Section");
        return sections;
    }

    public Section findSectionById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + id));
    }

    @Transactional
    public void deleteSection(Long id) {
        log.info(LogConstants.START_PROCESS, "deleteSection", id);
        Section section = findSectionById(id);
        sectionRepository.delete(section);
        log.info(LogConstants.COMPLETED_PROCESS, "deleteSection", id);
    }
}
