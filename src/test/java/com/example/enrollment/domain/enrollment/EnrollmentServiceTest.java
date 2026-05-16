package com.example.enrollment.domain.enrollment;


import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseRepository;
import com.example.enrollment.domain.course.CourseStatus;
import com.example.enrollment.domain.enrollment.EnrollmentRepository;
import com.example.enrollment.domain.enrollment.EnrollmentService;
import com.example.enrollment.domain.enrollment.EnrollmentStatus;
import com.example.enrollment.domain.enrollment.dto.EnrollmentRequest;
import com.example.enrollment.domain.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional  // 테스트 후 롤백 → DB 상태 초기화
class EnrollmentServiceTest {

    @Autowired
    EnrollmentService enrollmentService;
    @Autowired
    EnrollmentRepository enrollmentRepository;
    @Autowired CourseRepository courseRepository;

    private Course openCourse;

    @BeforeEach
    void setUp() {
        // OPEN 상태 강의 미리 생성
        Course course = Course.create(
                "테스트 강의", "설명", 50000, 30,
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 6, 30),
                "creator-1"
        );
        course.changeStatus(CourseStatus.OPEN);
        openCourse = courseRepository.save(course);
    }

    @Test
    @DisplayName("수강 신청 성공 시 PENDING 상태로 저장된다")
    void enroll_success() {
        EnrollmentRequest request = createRequest(openCourse.getId(), "student-1");

        EnrollmentResponse response = enrollmentService.enroll(request);

        assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(response.getCourseId()).isEqualTo(openCourse.getId());
    }

    @Test
    @DisplayName("수강 신청 시 강의의 enrolledCount가 증가한다")
    void enroll_increasesEnrolledCount() {
        EnrollmentRequest request = createRequest(openCourse.getId(), "student-1");
        enrollmentService.enroll(request);

        Course updated = courseRepository.findById(openCourse.getId()).orElseThrow();
        assertThat(updated.getEnrolledCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("중복 신청 시 예외가 발생한다")
    void enroll_duplicate_throwsException() {
        EnrollmentRequest request = createRequest(openCourse.getId(), "student-1");
        enrollmentService.enroll(request);

        assertThatThrownBy(() -> enrollmentService.enroll(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 신청한 강의");
    }

    @Test
    @DisplayName("결제 확정 시 CONFIRMED 상태로 변경된다")
    void confirm_success() {
        EnrollmentRequest request = createRequest(openCourse.getId(), "student-1");
        EnrollmentResponse enrolled = enrollmentService.enroll(request);

        EnrollmentResponse confirmed = enrollmentService.confirm(enrolled.getId());

        assertThat(confirmed.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(confirmed.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("수강 취소 시 CANCELLED 상태로 변경되고 enrolledCount가 감소한다")
    void cancel_success() {
        EnrollmentRequest request = createRequest(openCourse.getId(), "student-1");
        EnrollmentResponse enrolled = enrollmentService.enroll(request);

        enrollmentService.cancel(enrolled.getId());

        Course updated = courseRepository.findById(openCourse.getId()).orElseThrow();
        assertThat(updated.getEnrolledCount()).isZero();
    }

    @Test
    @DisplayName("내 신청 목록 조회 시 해당 학생의 신청만 반환된다")
    void findByStudent_success() {
        enrollmentService.enroll(createRequest(openCourse.getId(), "student-1"));
        enrollmentService.enroll(createRequest(openCourse.getId(), "student-2"));

        List<EnrollmentResponse> result = enrollmentService.findByStudent("student-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentId()).isEqualTo("student-1");
    }

    @Test
    @DisplayName("존재하지 않는 강의에 신청 시 예외가 발생한다")
    void enroll_courseNotFound_throwsException() {
        EnrollmentRequest request = createRequest(999L, "student-1");

        assertThatThrownBy(() -> enrollmentService.enroll(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("강의를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("DRAFT 상태 강의에는 신청할 수 없다")
    void enroll_draftCourse_throwsException() {
        Course draftCourse = Course.create(
                "초안 강의", "설명", 50000, 30,
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 6, 30),
                "creator-1"
        );
        courseRepository.save(draftCourse); // OPEN 전환 없이 저장

        EnrollmentRequest request = createRequest(draftCourse.getId(), "student-1");

        assertThatThrownBy(() -> enrollmentService.enroll(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("모집 중인 강의가 아닙니다");
    }

    // 요청 객체 생성 헬퍼
    private EnrollmentRequest createRequest(Long courseId, String studentId) {
        return new EnrollmentRequest(courseId, studentId);
    }
}
