package com.evalx.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateExamStageRequest {
    @NotNull(message = "Exam ID is required")
    private Long examId;

    @NotBlank(message = "Stage name is required")
    private String name;

    private String description;
    private Integer orderIndex;
}
