package com.example.enrollment.domain.course;

import com.example.enrollment.domain.course.dto.CourseRequest;
import com.example.enrollment.domain.course.dto.CourseResponse;
import com.example.enrollment.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
//조회 메서드가 대부분이라 기본값을 읽기 전용으로 설정함 -> 읽기 전용 트랜잭션은 변경 감지를 건너뜀
//변경 필요한 메서드에만 @Transactional을 따로 달면 됨
public class CourseService {

    private final CourseRepository courseRepository;

    //강의 등록
    @Transactional
    public CourseResponse create(CourseRequest request){
        if (request.getEndDate().isBefore(request.getStartDate())){
            throw new BusinessException("종료일은 시작일 이후여야 합니다.", HttpStatus.BAD_REQUEST);
        }
        Course course = Course.create(
                request.getTitle(),
                request.getDescription(),
                request.getPrice(),
                request.getCapacity(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCreatorId()
        );
        return CourseResponse.from(courseRepository.save(course));
    }

    //강의 목록 조회 (상태 필터)
    public List<CourseResponse> findAll(CourseStatus status) {
        List<Course> courses = (status != null)
                ? courseRepository.findByStatus(status)
                : courseRepository.findAll();

        return courses.stream()
                .map(CourseResponse::from)
                .toList();
    }

    // 강의 상세 조회
    public CourseResponse findById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "강의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return CourseResponse.from(course);
    }

    // 강의 상태 변경 (DRAFT → OPEN → CLOSED)
    @Transactional
    public CourseResponse changeStatus(Long id, CourseStatus newStatus) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "강의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 상태 전이 검증은 Course 엔티티 안에서 처리
        course.changeStatus(newStatus);

        return CourseResponse.from(course);
    }
}
