package com.example.enrollment.domain.course;

import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class CourseTest {

    private Course course;

    @BeforeEach
    void setUp() {
        course = Course.create(
                "Spring Boot 입문",
                "설명",
                50000,
                30,
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 6, 30),
                "creator-1"
        );
    }

    @Test
    @DisplayName("강의 생성 시 기본 상태는 DRAFT이다")
    void createCourse_defaultStatusIsDraft() {
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(course.getEnrolledCount()).isZero();
    }

    @Test
    @DisplayName("DRAFT → OPEN 상태 전이 성공")
    void changeStatus_draftToOpen_success() {
        course.changeStatus(CourseStatus.OPEN);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("OPEN → CLOSED 상태 전이 성공")
    void changeStatus_openToClosed_success() {
        course.changeStatus(CourseStatus.OPEN);
        course.changeStatus(CourseStatus.CLOSED);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.CLOSED);
    }

    @Test
    @DisplayName("DRAFT → CLOSED 직접 전이는 불가능하다")
    void changeStatus_draftToClosed_fail() {
        assertThatThrownBy(() -> course.changeStatus(CourseStatus.CLOSED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("변경할 수 없습니다");
    }

    @Test
    @DisplayName("OPEN → DRAFT 역방향 전이는 불가능하다")
    void changeStatus_openToDraft_fail() {
        course.changeStatus(CourseStatus.OPEN);
        assertThatThrownBy(() -> course.changeStatus(CourseStatus.DRAFT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("OPEN 상태에서 수강 신청 시 enrolledCount가 증가한다")
    void increaseEnrolledCount_success() {
        course.changeStatus(CourseStatus.OPEN);
        course.increaseEnrolledCount();
        assertThat(course.getEnrolledCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("DRAFT 상태에서는 수강 신청이 불가능하다")
    void increaseEnrolledCount_draftStatus_fail() {
        assertThatThrownBy(() -> course.increaseEnrolledCount())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("모집 중인 강의가 아닙니다");
    }

    @Test
    @DisplayName("정원 초과 시 신청이 거부된다")
    void increaseEnrolledCount_overCapacity_fail() {
        // 정원 1명짜리 강의 생성
        Course smallCourse = Course.create(
                "소규모 강의", "설명", 50000, 1,
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 6, 30),
                "creator-1"
        );
        smallCourse.changeStatus(CourseStatus.OPEN);
        smallCourse.increaseEnrolledCount(); // 1번째 → 성공

        assertThatThrownBy(() -> smallCourse.increaseEnrolledCount()) // 2번째 → 실패
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("정원이 초과");
    }

    @Test
    @DisplayName("취소 시 enrolledCount가 감소한다")
    void decreaseEnrolledCount_success() {
        course.changeStatus(CourseStatus.OPEN);
        course.increaseEnrolledCount();
        course.decreaseEnrolledCount();
        assertThat(course.getEnrolledCount()).isZero();
    }
}
