package com.example.enrollment.domain.enrollment;

import com.example.enrollment.domain.course.Course;
import com.example.enrollment.domain.course.CourseRepository;
import com.example.enrollment.domain.course.CourseStatus;
import com.example.enrollment.domain.enrollment.dto.EnrollmentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ConcurrencyTest {

    @Autowired
    EnrollmentService enrollmentService;
    @Autowired
    CourseRepository courseRepository;
    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("정원 5명인 강의에 10명이 동시 신청하면 정확히 5명만 성공한다")
    void concurrentEnroll_onlyCapacitySucceeds() throws InterruptedException {
        // 정원 5명짜리 강의 생성
        Course course = Course.create(
                "동시성 테스트 강의", "설명", 50000, 5,
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 6, 30),
                "creator-1"
        );
        course.changeStatus(CourseStatus.OPEN);
        Course savedCourse = courseRepository.save(course);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount); // 준비 완료용
        CountDownLatch startLatch = new CountDownLatch(1); // 동시 출발용

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String studentId = "student-" + i;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();       // 준비 완료 신호
                    startLatch.await();           // 모든 스레드가 준비될 때까지 대기
                    enrollmentService.enroll(
                            new EnrollmentRequest(savedCourse.getId(), studentId));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); //전부 준비되면 동시 출발

        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        // 성공은 정원(5명)만큼만
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);

        // DB의 enrolledCount도 정확히 5인지 확인
        Course result = courseRepository.findById(savedCourse.getId()).orElseThrow();
        assertThat(result.getEnrolledCount()).isEqualTo(5);

        // 테스트 후 직접 정리
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 학생이 동시에 중복 신청해도 하나만 성공한다")
    void concurrentEnroll_sameStudent_onlyOneSucceeds() throws InterruptedException {
        Course course = Course.create(
                "중복 테스트 강의", "설명", 50000, 30,
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 6, 30),
                "creator-1"
        );
        course.changeStatus(CourseStatus.OPEN);
        Course savedCourse = courseRepository.save(course);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    // 같은 studentId로 동시 신청
                    enrollmentService.enroll(
                            new EnrollmentRequest(savedCourse.getId(), "same-student"));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 중복 신청 실패는 정상
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
    }
}