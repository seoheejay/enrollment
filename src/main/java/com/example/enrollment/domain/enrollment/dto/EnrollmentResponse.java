package com.example.enrollment.domain.enrollment.dto;

import com.example.enrollment.domain.enrollment.Enrollment;
import com.example.enrollment.domain.enrollment.EnrollmentStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EnrollmentResponse {

    private Long id;
    private Long courseId;
    private String studentId;
    private EnrollmentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private boolean cancellable;

    public static EnrollmentResponse from(Enrollment enrollment) {
        EnrollmentResponse dto = new EnrollmentResponse();
        dto.id = enrollment.getId();
        dto.courseId = enrollment.getCourseId();
        dto.studentId = enrollment.getStudentId();
        dto.status = enrollment.getStatus();
        dto.paidAt = enrollment.getPaidAt();
        dto.cancelledAt = enrollment.getCancelledAt();
        dto.createdAt = enrollment.getCreatedAt();
        dto.cancellable = enrollment.isCancellable();
        return dto;
    }
}
