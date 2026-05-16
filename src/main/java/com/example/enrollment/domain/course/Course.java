package com.example.enrollment.domain.course;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //protected로 해서 빈 객체 만들어지지 않도록, 정적 팩토리 메서드로만 만들어지도록 강제
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    private String title;
    private String description;
    private int price;
    private int capacity; //최대정원
    private int enrolledCount; //현재 신청 인원

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private CourseStatus status; //DRAFT/OPEN/CLOSED

    private String creatorId;

    @Version
    private Long version; //낙관적 락 사용

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //생성자(정적 팩토리 메서드)
    public static Course create(String title, String description, int price, int capacity, LocalDate startDate, LocalDate endDate, String creatorId){
        Course course = new Course();
        course.title = title;
        course.description = description;
        course.price = price;
        course.capacity = capacity;
        course.enrolledCount = 0;
        course.startDate = startDate;
        course.endDate = endDate;
        course.status = CourseStatus.DRAFT; //default 값 항상 DRAFT
        course.creatorId = creatorId;
        course.createdAt = LocalDateTime.now();
        course.updatedAt = LocalDateTime.now();
        return course;
    }

    //상태 전이
    public void changeStatus(CourseStatus newStatus){
        validateStatusTransition(newStatus);
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateStatusTransition(CourseStatus newStatus) {
        //허용된 전이만 통과 가능
        //DRAFT -> OPEN 가능
        //OPEN -> CLOSE 가능
        //나머지는 예외
        boolean valid = switch (this.status){
            case DRAFT -> newStatus == CourseStatus.OPEN;
            case OPEN -> newStatus == CourseStatus.CLOSED;
            case CLOSED -> false;
        };
        if (!valid){
            throw new IllegalStateException(
                    "강의 상태를 " + this.status + "에서" + newStatus + "로 변경할 수 없습니다."
            );
        }
    }

    //수강 신청 시 호출 - 정원 초과 체크 + 카운트 증가
    public void increaseEnrolledCount(){
        if (this.enrolledCount >= this.capacity){
            throw new IllegalStateException("정원이 초과되었습니다.");
        }
        if (this.status != CourseStatus.OPEN){
            throw new IllegalStateException("모집 중인 강의가 아닙니다.");
        }
        this.enrolledCount++;
        this.updatedAt = LocalDateTime.now();
    }

    //수강 취소 시 호출
    public void decreaseEnrolledCount(){
        if (this.enrolledCount > 0){
            this.enrolledCount--;
            this.updatedAt = LocalDateTime.now();
        }

    }


}
