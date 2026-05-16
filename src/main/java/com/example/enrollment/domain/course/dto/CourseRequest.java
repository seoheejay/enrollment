package com.example.enrollment.domain.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CourseRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @Min(value = 0, message = "가격은 0이상이어야 합니다.")
    private int price;

    @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
    private int capacity;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    @NotBlank(message = "크리에이터 ID는 필수입니다.")
    private String creatorId;
}
