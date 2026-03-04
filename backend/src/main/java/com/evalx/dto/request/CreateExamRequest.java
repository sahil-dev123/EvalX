package com.evalx.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateExamRequest {
    @NotBlank(message = "Exam name is required")
    private String name;

    @NotBlank(message = "Exam code is required")
    private String code;

    private String description;
    private String iconUrl;
}
