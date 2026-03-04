package com.evalx.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateSectionRequest {
    @NotNull(message = "Exam Year ID is required")
    private Long examYearId;

    @NotBlank(message = "Section name is required")
    private String name;

    private Integer totalQuestions;
    private Integer orderIndex;
}
