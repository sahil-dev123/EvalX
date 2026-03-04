package com.evalx.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateShiftRequest {
    @NotNull(message = "Exam Year ID is required")
    private Long examYearId;

    @NotBlank(message = "Shift name is required")
    private String name;

    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
