package com.example.enrollment.domain.enrollment;

public enum EnrollmentStatus {
    PENDING,    //신청 완료, 결제 대기
    CONFIRMED, //결제 완료, 수강 확정
    CANCELLED //취소됨
}
