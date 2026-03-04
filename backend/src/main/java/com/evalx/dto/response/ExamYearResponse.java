package com.evalx.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamYearResponse {
    private Long id;
    private Long examStageId;
    private String stageName;
    private String examName;
    private Integer year;
    private Long totalCandidates;
    private Double totalMarks;
    private Integer timeMinutes;
    private List<ShiftInfo> shifts;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ShiftInfo {
        private Long id;
        private String name;
        private java.time.LocalDate shiftDate;
    }
}
