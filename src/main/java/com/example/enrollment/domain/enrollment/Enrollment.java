package com.example.enrollment.domain.enrollment;

import jakarta.persistence.*;
import jakarta.validation.constraints.Null;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long courseId;
    private String studentId;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    private LocalDateTime paidAt; //결제 확정 시각
    private LocalDateTime cancelledAt; //취소 시각
    private LocalDateTime createdAt;

    public static Enrollment create(Long courseId, String studentId){
        Enrollment enrollment = new Enrollment();
        enrollment.courseId = courseId;
        enrollment.studentId = studentId;
        enrollment.status = EnrollmentStatus.PENDING; //default값 PENDING
        enrollment.createdAt = LocalDateTime.now();
        return enrollment;
    }

    //결제확정: PENDING -> CONFIRMED
    public void confirm(){
        if (this.status != EnrollmentStatus.PENDING){
            throw new IllegalStateException("결제 대기 상태에서만 확정할 수 있습니다.");
        }
        this.status = EnrollmentStatus.CONFIRMED;
        this.paidAt = LocalDateTime.now();
    }

    //수강 취소 : PENDING or CONFIRMED -> CANCELLED
    public void cancel(){
        if (this.status == EnrollmentStatus.CANCELLED){
            throw new IllegalStateException("이미 취소된 신청입니다.");
        }

        //Confiremd 상태 (결제 완료)인 경우만 기간 제한 적용
        //Pendig은 아직 결제 전이므로 기간 제한 없이 취소 가능
        if (this.status == EnrollmentStatus.CONFIRMED){
            if (this.paidAt == null ||
                    this.paidAt.plusDays(7).isBefore(LocalDateTime.now())) {
                throw new IllegalStateException(
                        "결제 후 7일이 지나 취소할 수 없습니다.");
            }
        }
        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isCancellable(){
        if (this.status == EnrollmentStatus.CANCELLED) return false;
        if (this.status == EnrollmentStatus.PENDING) return true; //결제 전이라 기간 제한 없이 취소 가능

        //Confirmed는 결제 후 7일 이내만 가능
        return this.paidAt != null &&
                this.paidAt.plusDays(7).isAfter(LocalDateTime.now());
    }
}
