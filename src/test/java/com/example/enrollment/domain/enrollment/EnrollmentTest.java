package com.example.enrollment.domain.enrollment;

import com.example.enrollment.domain.enrollment.Enrollment;
import com.example.enrollment.domain.enrollment.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EnrollmentTest {

    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        enrollment = Enrollment.create(1L, "student-1");
    }

    @Test
    @DisplayName("수강 신청 생성 시 기본 상태는 PENDING이다")
    void createEnrollment_defaultStatusIsPending() {
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    @DisplayName("PENDING → CONFIRMED 결제 확정 성공")
    void confirm_pendingToConfirmed_success() {
        enrollment.confirm();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(enrollment.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("CONFIRMED 상태에서 다시 confirm 호출 시 예외 발생")
    void confirm_alreadyConfirmed_fail() {
        enrollment.confirm();
        assertThatThrownBy(() -> enrollment.confirm())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("결제 대기 상태에서만");
    }

    @Test
    @DisplayName("PENDING 상태에서 취소 가능하다")
    void cancel_pendingStatus_success() {
        enrollment.cancel();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollment.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("CONFIRMED 상태에서 7일 이내 취소 가능하다")
    void cancel_confirmedWithin7Days_success() {
        enrollment.confirm();
        enrollment.cancel(); // 방금 confirm했으니 7일 이내
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("이미 취소된 신청은 다시 취소할 수 없다")
    void cancel_alreadyCancelled_fail() {
        enrollment.cancel();
        assertThatThrownBy(() -> enrollment.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 취소된");
    }

    @Test
    @DisplayName("PENDING 상태는 취소 가능하다")
    void isCancellable_pendingStatus_true() {
        assertThat(enrollment.isCancellable()).isTrue();
    }

    @Test
    @DisplayName("CANCELLED 상태는 취소 불가능하다")
    void isCancellable_cancelledStatus_false() {
        enrollment.cancel();
        assertThat(enrollment.isCancellable()).isFalse();
    }
}
