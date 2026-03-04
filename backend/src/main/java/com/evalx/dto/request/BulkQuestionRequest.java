package com.evalx.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkQuestionRequest {
    @NotNull(message = "Section ID is required")
    private Long sectionId;

    @NotNull(message = "Questions list is required")
    private List<QuestionItem> questions;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuestionItem {
        private Long questionNumber;
        private String questionText;
        private String questionHash;
        private String questionType;
        private String correctAnswer;
    }
}
