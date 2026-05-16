package com.example.enrollment.domain.course;

import com.example.enrollment.domain.course.dto.CourseRequest;
import com.example.enrollment.domain.course.dto.CourseResponse;
import com.example.enrollment.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    //강의 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseResponse> create(@Valid @RequestBody CourseRequest request){
        return ApiResponse.ok(courseService.create(request));
    }

    //강의 목록 조회
    // GET /api/courses
    // GET /api/courses?status=OPEN
    @GetMapping
    public ApiResponse<List<CourseResponse>> findAll(
            @RequestParam(required = false) CourseStatus status){
        return ApiResponse.ok(courseService.findAll(status));
    }

    //강의 상태 변경
    // PATCH /api/courses/{id}/status
    // Body: {"status": "OPEN"}
    @PatchMapping("/{id}/status")
    public ApiResponse<CourseResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam CourseStatus status){
        return ApiResponse.ok(courseService.changeStatus(id, status));
    }

}
