package com.example.enrollment.domain.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    //내 신청 목록 조회
    List<Enrollment> findByStudentId(String studentId);

    //특정 강의의 특정 학생 신청 여부 확인 (중복 신청 방지)
    Optional<Enrollment> findByCourseIdAndStudentId(Long courseId, String studentId);

    //강의별 수강생 목록(크리에이터 전용)
    List<Enrollment> findByCourseId(Long courseId);
}
