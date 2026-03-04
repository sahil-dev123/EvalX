package com.evalx.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubmitResponseRequest {
    @NotNull(message = "Exam Year ID is required")
    private Long examYearId;

    @NotNull(message = "Responses are required")
    private List<ResponseItem> responses;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ResponseItem {
        private Integer questionId;
        private String answer;
    }
}
