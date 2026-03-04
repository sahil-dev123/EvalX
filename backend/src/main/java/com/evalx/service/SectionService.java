package com.evalx.service;

import com.evalx.dto.request.CreateSectionRequest;
import com.evalx.entity.Shift;
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
    private final ShiftService shiftService;

    @Transactional
    public Section createSection(CreateSectionRequest request) {
        Shift shift = shiftService.findShiftById(request.getShiftId());
        Section section = Section.builder()
                .shift(shift)
                .name(request.getName())
                .totalQuestions(request.getTotalQuestions())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();
        return sectionRepository.save(section);
    }

    public List<Section> getSectionsByShiftId(Long shiftId) {
        return sectionRepository.findByShiftIdOrderByOrderIndexAsc(shiftId);
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
