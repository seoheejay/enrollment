package com.example.enrollment.domain.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class EnrollmentRequest {

    @NotNull(message = "강의ID는 필수입니다.")
    private Long courseId;

    @NotBlank(message = "수강생 ID는 필수입니다.")
    private String studentId;

    // 테스트 편의용 생성자
    public EnrollmentRequest(Long courseId, String studentId) {
        this.courseId = courseId;
        this.studentId = studentId;
    }

}

