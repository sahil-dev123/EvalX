package com.evalx.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateExamYearRequest {
    @NotNull(message = "Exam Stage ID is required")
    private Long examStageId;

    @NotNull(message = "Year is required")
    private Integer year;

    private Long totalCandidates;
    private Double totalMarks;
    private Integer timeMinutes;
}
