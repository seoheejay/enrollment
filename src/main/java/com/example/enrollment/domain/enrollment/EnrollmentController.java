package com.example.enrollment.domain.enrollment;

import com.example.enrollment.domain.enrollment.dto.EnrollmentRequest;
import com.example.enrollment.domain.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // 수강 신청
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollmentRequest request) {
        return ApiResponse.ok(enrollmentService.enroll(request));
    }

    // 결제 확정
    @PatchMapping("/{id}/confirm")
    public ApiResponse<EnrollmentResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok(enrollmentService.confirm(id));
    }

    // 수강 취소
    @PatchMapping("/{id}/cancel")
    public ApiResponse<EnrollmentResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(enrollmentService.cancel(id));
    }

    // 내 신청 목록 조회
    // GET /api/enrollments?studentId=student-1
    @GetMapping
    public ApiResponse<List<EnrollmentResponse>> findByStudent(
            @RequestParam String studentId) {
        return ApiResponse.ok(enrollmentService.findByStudent(studentId));
    }
}
