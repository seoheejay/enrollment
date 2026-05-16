# 수강 신청 시스템 (BE)

## 프로젝트 개요

크리에이터가 강의를 개설하고, 수강생이 신청·결제·취소하는 수강 신청 시스템입니다.  
정원 관리와 동시 신청 처리를 핵심 요구사항으로 구현했습니다.

---

## 기술 스택

| 항목 | 선택 | 이유 |
|---|---|---|
| Language | Java 17 | 안정적인 LTS 버전 |
| Framework | Spring Boot 3.3 | 과제 필수 스택 |
| ORM | Spring Data JPA | 선언적 락 처리 및 변경 감지 활용 |
| DB | H2 (in-memory) | 별도 설치 없이 즉시 실행 가능, 평가 편의성 고려 |
| Build | Gradle | |

---

## 실행 방법

### 사전 요구사항
- Java 17 이상
- 별도 DB 설치 불필요 (H2 인메모리 사용)

### 실행

```bash
./gradlew bootRun
```

서버 시작 후 `http://localhost:8080` 으로 접근 가능합니다.

### H2 콘솔 (DB 직접 확인)
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:enrollmentdb
Username: sa
Password: (없음)

### 테스트 실행

```bash
./gradlew test
```

결과 리포트:
build/reports/tests/test/index.html

---

## 요구사항 해석 및 가정

### 인증/인가
- 과제 안내에 따라 `studentId`, `creatorId`를 요청 파라미터/바디로 직접 전달하는 방식으로 구현했습니다.
- 실제 서비스라면 JWT 또는 세션 기반 인증을 적용할 것입니다.

### 결제 시스템
- 외부 결제 연동은 구현 범위 외이므로, `PATCH /api/enrollments/{id}/confirm` 호출로 결제 완료를 대체했습니다.

### 취소 가능 기간
- 과제 선택 구현 항목인 "결제 후 7일 이내 취소 가능" 규칙을 적용했습니다.
- PENDING(결제 전) 상태는 기간 제한 없이 취소 가능합니다.
- CONFIRMED(결제 완료) 상태는 paidAt 기준 7일 이내에만 취소 가능합니다.

### 중복 신청
- CANCELLED 상태의 신청이 있는 경우 재신청을 허용합니다.
- PENDING 또는 CONFIRMED 상태의 신청이 있는 경우 중복 신청으로 간주하고 거부합니다.

---

## 설계 결정과 이유

### 1. 도메인 모델 패턴 (Rich Domain Model)

비즈니스 로직을 Service가 아닌 Entity 안에 배치했습니다.

```java
// Course.java
public void increaseEnrolledCount() {
    if (this.enrolledCount >= this.capacity) {
        throw new IllegalStateException("정원이 초과되었습니다.");
    }
    ...
}
```

- 로직이 데이터와 함께 있어 응집도가 높습니다.
- Service 코드가 얇아져 가독성이 좋습니다.
- Entity 단위 테스트가 DB 없이 가능합니다.

### 2. 비관적 락으로 동시성 제어

수강 신청 시 `PESSIMISTIC_WRITE` 락을 적용했습니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Course c WHERE c.id = :id")
Optional<Course> findByIdWithLock(Long id);
```

**낙관적 락 대신 비관적 락을 선택한 이유**

초기 설계는 `@Version` 기반 낙관적 락으로 시작했습니다.
낙관적 락은 충돌이 적을 때 성능상 유리하지만, 수강 신청은 특정 시간에 요청이 집중되는 구조라 충돌 빈도가 높습니다.
또한 H2 인메모리 DB 환경에서 낙관적 락 충돌 후 재시도 로직이 없으면 정원만큼 신청이 채워지지 않는 문제가 있었습니다.

| 구분 | 낙관적 락 | 비관적 락 |
|---|---|---|
| 방식 | 충돌 시 예외 발생 | DB 레벨 락 선점 |
| 성능 | 충돌 적을 때 유리 | 충돌 많을 때 유리 |
| 재시도 | 애플리케이션 레벨 필요 | 불필요 |
| 적합한 상황 | 충돌이 드문 일반 업무 | 수강신청처럼 경쟁이 몰리는 상황 |

수강 신청 특성상 비관적 락이 더 적합하다고 판단했습니다.

### 3. 도메인별 패키지 구조

계층형(controller/service/repository) 대신 도메인별로 패키지를 구성했습니다.
```text
src/main/java/com/example/enrollment/domain/
├── course/
│   ├── Course.java
│   ├── CourseController.java
│   ├── CourseRepository.java
│   └── CourseService.java
└── enrollment/
    ├── Enrollment.java
    ├── EnrollmentController.java
    ├── EnrollmentRepository.java
    └── EnrollmentService.java
```

기능이 추가될 때 관련 파일이 한 곳에 모여 있어 수정 범위 파악이 쉽습니다.

### 4. 공통 응답 포맷

모든 API 응답을 `ApiResponse<T>`로 감쌌습니다.

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

성공/실패를 일관된 구조로 처리해 클라이언트가 예측 가능하게 응답을 처리할 수 있습니다.

---

## API 목록 및 예시

### 강의 관리

#### 강의 등록
POST /api/courses
```json
// Request
{
  "title": "Spring Boot 입문",
  "description": "스프링 기초 강의",
  "price": 50000,
  "capacity": 30,
  "startDate": "2025-04-01",
  "endDate": "2025-06-30",
  "creatorId": "creator-1"
}

// Response 201
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Spring Boot 입문",
    "status": "DRAFT",
    "enrolledCount": 0,
    "remainingCount": 30
  },
  "message": null
}
```

#### 강의 목록 조회
GET /api/courses
GET /api/courses?status=OPEN

#### 강의 상세 조회
GET /api/courses/{id}

#### 강의 상태 변경
PATCH /api/courses/{id}/status?status=OPEN

허용된 전이: `DRAFT → OPEN → CLOSED`

---

### 수강 신청 관리

#### 수강 신청
POST /api/enrollments
```json
// Request
{
  "courseId": 1,
  "studentId": "student-1"
}

// Response 201
{
  "success": true,
  "data": {
    "id": 1,
    "courseId": 1,
    "studentId": "student-1",
    "status": "PENDING",
    "cancellable": true,
    "paidAt": null,
    "cancelledAt": null
  },
  "message": null
}
```

#### 결제 확정
PATCH /api/enrollments/{id}/confirm
`PENDING → CONFIRMED`

#### 수강 취소
PATCH /api/enrollments/{id}/cancel
- PENDING: 기간 제한 없이 취소 가능
- CONFIRMED: 결제 후 7일 이내만 취소 가능

#### 내 신청 목록
GET /api/enrollments?studentId=student-1

---

### 에러 응답 형식

```json
{
  "success": false,
  "data": null,
  "message": "정원이 초과되었습니다."
}
```

| HTTP 상태 | 상황 |
|---|---|
| 400 | 유효성 검증 실패, 잘못된 상태 전이, 정원 초과 |
| 404 | 강의/신청 내역 없음 |
| 409 | 중복 신청, 동시성 충돌 |
| 500 | 서버 내부 오류 |

---

## 데이터 모델 설명

### Course (강의)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| title | String | 강의 제목 |
| description | String | 강의 설명 |
| price | int | 가격 |
| capacity | int | 최대 정원 |
| enrolledCount | int | 현재 신청 인원 |
| startDate | LocalDate | 수강 시작일 |
| endDate | LocalDate | 수강 종료일 |
| status | Enum | DRAFT / OPEN / CLOSED |
| creatorId | String | 강사 ID |
| version | Long | 낙관적 락 설계용 버전 필드 (현재 비관적 락 사용) |

### Enrollment (수강 신청)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| courseId | Long | 강의 ID (FK) |
| studentId | String | 수강생 ID |
| status | Enum | PENDING / CONFIRMED / CANCELLED |
| paidAt | LocalDateTime | 결제 확정 시각 |
| cancelledAt | LocalDateTime | 취소 시각 |
| createdAt | LocalDateTime | 신청 시각 |

### 상태 전이 다이어그램
graph TD
    subgraph 강의 상태 전이
        DRAFT --> OPEN --> CLOSED
    end

    subgraph 수강 신청 상태 전이
        PENDING --> CONFIRMED
        PENDING -->|기간 제한 없음| CANCELLED
        CONFIRMED -->|7일 이내만| CANCELLED
    end

---

## 테스트 실행 방법

```bash
./gradlew test
```

### 테스트 구성

| 테스트 파일 | 종류 | 설명 |
|---|---|---|
| CourseTest | 단위 | 강의 상태 전이, 정원 관리 로직 |
| EnrollmentTest | 단위 | 신청 상태 전이, 취소 기간 제한 |
| EnrollmentServiceTest | 통합 | 서비스 레이어 + DB 연동 |
| ConcurrencyTest | 동시성 | 10명 동시 신청 시 정원만큼만 성공 확인 |

---

## 미구현 / 제약사항

- **인증/인가**: userId를 파라미터로 직접 전달 (과제 안내에 따름)
- **대기열(waitlist)**: 선택 구현 항목으로 미구현
- **강의별 수강생 목록**: 선택 구현 항목으로 미구현
- **페이지네이션**: 선택 구현 항목으로 미구현
- **실제 결제 연동**: 단순 상태 변경으로 대체

---

## AI 활용 범위

본 과제는 Claude(Anthropic)를 활용하여 개발했습니다.

### 활용 내역

| 영역 | 활용 방식 |
|---|---|
| 전체 설계 | 패키지 구조, ERD, API 목록 초안 제안 → 직접 검토 후 확정 |
| 엔티티 코드 | 초안 생성 → 상태 전이 로직, 락 구조 직접 검증 |
| 서비스 코드 | 초안 생성 → 동시성 처리 흐름 직접 이해 후 수정 |
| 테스트 코드 | 초안 생성 → 실패 케이스 직접 디버깅 및 수정 |
| README | 초안 생성 → 설계 결정 이유 직접 작성 |

### 직접 판단한 부분

- 낙관적 락 vs 비관적 락 선택 이유 (수강 신청 특성상 경쟁 집중)
- 도메인 모델 패턴 적용 여부
- 취소 기간 제한 정책 해석 (PENDING은 제한 없음)
- 중복 신청 허용 조건 (CANCELLED 후 재신청 가능)
- 동시성 테스트 실패 원인 분석 및 해결 방향 결정
