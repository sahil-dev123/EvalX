package com.evalx.dto.request;

import com.evalx.entity.QuestionType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateQuestionRequest {
    @NotNull(message = "Section ID is required")
    private Long sectionId;

    private Long questionNumber;

    private QuestionType questionType;
    private String correctAnswer;
}
