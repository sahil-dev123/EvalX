package com.evalx.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateMarkingPolicyRequest {
    @NotNull(message = "Exam Year ID is required")
    private Long examYearId;

    private Long sectionId;

    @NotNull(message = "Correct marks required")
    private Double correctMarks;

    @NotNull(message = "Negative marks required")
    private Double negativeMarks;

    private Double unattemptedMarks;
}
