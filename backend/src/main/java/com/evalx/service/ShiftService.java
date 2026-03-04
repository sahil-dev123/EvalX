package com.evalx.service;

import com.evalx.dto.request.CreateShiftRequest;
import com.evalx.entity.ExamYear;
import com.evalx.entity.Shift;
import com.evalx.exception.ResourceNotFoundException;
import com.evalx.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final ExamYearService examYearService;

    @Transactional
    public Shift createShift(CreateShiftRequest request) {
        ExamYear examYear = examYearService.findExamYearById(request.getExamYearId());
        Shift shift = Shift.builder()
                .examYear(examYear)
                .name(request.getName())
                .shiftDate(request.getShiftDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        return shiftRepository.save(shift);
    }

    public List<Shift> getShiftsByExamYearId(Long examYearId) {
        return shiftRepository.findByExamYearIdOrderByNameAsc(examYearId);
    }

    public Shift findShiftById(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with id: " + id));
    }
}
