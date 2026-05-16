package com.example.enrollment.domain.course;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    //상태 필터 조회 (status가 null이면 전체)
    List<Course> findByStatus(CourseStatus status);


    //@Lock(LockModeType.OPTIMISTIC)
    @Lock(LockModeType.PESSIMISTIC_WRITE)  // OPTIMISTIC → PESSIMISTIC_WRITE
    @Query("SELECT c from Course c Where c.id = :id")
    Optional<Course> findByIdWithLock(Long id);
}
