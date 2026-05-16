package com.example.enrollment.domain.course.dto;

import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseStatus;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private int price;
    private int capacity;
    private int enrolledCount;
    private int remainingCount; //남은 자리
    private LocalDate startDate;
    private LocalDate endDate;
    private CourseStatus status;
    private String creatorId;
    private LocalDateTime createdAt;

    // 엔티티 -> DTO 변환
    public static CourseResponse from(Course course){
        CourseResponse dto = new CourseResponse();
        dto.id = course.getId();
        dto.title = course.getTitle();
        dto.description = course.getDescription();
        dto.price = course.getPrice();
        dto.capacity = course.getCapacity();
        dto.enrolledCount = course.getEnrolledCount();
        dto.remainingCount = course.getCapacity() - course.getEnrolledCount();
        dto.startDate = course.getStartDate();
        dto.endDate = course.getEndDate();
        dto.status = course.getStatus();
        dto.creatorId = course.getCreatorId();
        dto.createdAt = course.getCreatedAt();
        return dto;
    }

}
