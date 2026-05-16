package com.example.enrollment.domain.enrollment;

import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseRepository;
import com.example.enrollment.domain.enrollment.dto.EnrollmentRequest;
import com.example.enrollment.domain.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    // 수강 신청
    @Transactional
    public EnrollmentResponse enroll(EnrollmentRequest request){
        try {
            //낙관적 락 걸고 조회 - 동시 신청 시 한 명만 성공
            Course course = courseRepository.findByIdWithLock(request.getCourseId())
                    .orElseThrow(() -> new BusinessException(
                            "강의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
            //중복 신청 방지
            enrollmentRepository.findByCourseIdAndStudentId(
                    request.getCourseId(), request.getStudentId())
                    .ifPresent(e -> {
                        if (e.getStatus() != EnrollmentStatus.CANCELLED){
                            throw  new BusinessException(
                                    "이미 신청한 강의입니다.",HttpStatus.CONFLICT);
                        }
                    });
            //정원 체크 + 카운트 증가
            course.increaseEnrolledCount(); //Course엔티티 안에서 처리

            Enrollment enrollment = Enrollment.create(
                    request.getCourseId(),
                    request.getStudentId()
            );
            return EnrollmentResponse.from(enrollmentRepository.save(enrollment));

        } catch (ObjectOptimisticLockingFailureException e){
            //동시 신청으로 낙관적 락 충돌 발생 시
            throw new BusinessException("현재 신청이 집중되고 있습니다. 잠시 후 다시 시도해주세요.",
                    HttpStatus.CONFLICT);
        }
    }

    // 결제 확정: PENDING → CONFIRMED
    @Transactional
    public EnrollmentResponse confirm(Long enrollmentId){
        Enrollment enrollment = findEnrollmentById(enrollmentId);
        enrollment.confirm();
        return EnrollmentResponse.from(enrollment);
    }

    // 수강 취소
    @Transactional
    public EnrollmentResponse cancel(Long enrollmentId){
        Enrollment enrollment = findEnrollmentById(enrollmentId);

        //취소 시 강의 정원 복구
        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new BusinessException(
                        "강의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        enrollment.cancel();
        course.decreaseEnrolledCount();

        return EnrollmentResponse.from(enrollment);
    }

    // 내 신청 목록 조회
    public List<EnrollmentResponse> findByStudent(String studentId){
        return enrollmentRepository.findByStudentId(studentId)
                .stream()
                .map(EnrollmentResponse::from)
                .toList();
    }

    // 공통 - 신청 내역 조회
    private Enrollment findEnrollmentById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "신청내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}
