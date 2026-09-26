# ai-tms 기본 골격

## 1. 폴더 구조

```
ai-tms/
├── backend/                        # Spring Boot 3.x, Java 21, Gradle
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/aitms/
│       │   ├── AiTmsApplication.java
│       │   ├── common/              # 공통 응답, 예외 처리
│       │   ├── config/              # CORS, H2, JPA 설정
│       │   └── domain/
│       │       ├── project/         # Project 엔티티/리포지토리/서비스/컨트롤러
│       │       ├── requirement/     # Requirement, AtomicRequirement
│       │       ├── testcase/        # TestCase, RuleCatalog
│       │       ├── testrun/         # TestRound, TestRoundCase
│       │       └── defect/          # Defect
│       └── resources/
│           ├── application.yml
│           ├── schema.sql           # 테이블 정의 (동봉)
│           └── data.sql             # 초기 시드 데이터 (동봉)
│
└── frontend/                       # Vue 3 + Vite
    └── src/
        ├── views/
        │   ├── DashboardView.vue        # 1. 대시보드
        │   ├── TestCaseRepoView.vue     # 2. 테스트케이스 저장소
        │   ├── TestRunView.vue          # 3. 테스트수행관리
        │   └── DefectView.vue           # 4. 결함등록
        ├── components/
        ├── stores/                 # Pinia
        ├── router/
        └── styles/
            └── theme.css           # 라이트/다크 CSS 변수 토큰
```

## 2. 도메인 엔티티 (기본 골격)

| 엔티티 | 주요 필드 | 비고 |
|---|---|---|
| `Project` | id, name, description | 프로젝트 단위 |
| `Requirement` | id, project_id, raw_text, source(manual/pms) | PMS 연동 전이라 source=manual 고정 |
| `AtomicRequirement` | id, requirement_id, atomic_text, type, min_value, max_value, unit, conditions | AI 에이전트가 분해한 원자 요구사항 |
| `TestCase` | id, atomic_requirement_id, title, steps, expected_result, technique, source(rule/rag/llm), status(draft/approved/rejected) | source로 추천 출처 구분 |
| `RuleCatalog` | id, requirement_type, technique, template | 규칙기반 추천용 (초기 타입: amount_range, rate_range, period_condition, boolean_flag) |
| `TestRound` | id, project_id, name, start_date, end_date, status | "차수" |
| `TestRoundCase` | id, test_round_id, test_case_id, result(success/fail/block/not_executed), executed_by, executed_at, comment | 차수별 테스트케이스 실행 결과 |
| `Defect` | id, test_round_case_id, title, description, severity, status, assignee | 실패 케이스에서 결함 등록 |

## 3. 기능별 REST 엔드포인트 (초안)

**1. 대시보드**
- `GET /api/dashboard/my-tasks` — 로그인 사용자 담당 TC/결함 요약 (인증 붙기 전까지는 전체 or 파라미터로 필터)

**2. 테스트케이스 저장소**
- `GET/POST /api/test-cases`
- `GET/PUT/DELETE /api/test-cases/{id}`
- `POST /api/requirements` — 요구사항 등록
- `POST /api/requirements/{id}/recommend` — AI 추천 트리거 (추후 LangGraph 연동)

**3. 테스트수행관리**
- `GET/POST /api/test-rounds`
- `POST /api/test-rounds/{id}/cases` — 차수에 테스트케이스 등록
- `PATCH /api/test-round-cases/{id}` — 결과 업데이트(성공/실패/block/미수행)

**4. 결함등록**
- `GET/POST /api/defects`
- `GET/PUT/DELETE /api/defects/{id}`

## 4. 진행 순서 제안

1. `schema.sql` 기반으로 JPA 엔티티 생성 (Lombok + JPA Auditing)
2. 도메인별 Repository → Service → Controller 순으로 1개 기능씩
3. 프론트: Vue 라우터 + 4개 View 뼈대 + 라이트/다크 테마 토큰 세팅
4. `data.sql`(동봉) 로 시드 데이터 넣고 프론트-백엔드 연결 확인
5. AI 에이전트(LangGraph)는 이 골격이 안정된 후 별도로 붙이기

## 5. 사용 방법

이 폴더의 `01-schema.sql`, `02-sample-data.sql` 을
`backend/src/main/resources/schema.sql`, `backend/src/main/resources/data.sql` 로 그대로 복사하면
Spring Boot 기동 시 H2에 테이블 생성 + 예시 데이터가 자동으로 들어갑니다.

Claude Code 세션에 아래처럼 요청하면 됩니다:

```
동봉한 schema.sql, data.sql 파일을 참고해서
backend/src/main/resources/에 넣고, 이 스키마에 맞는 JPA 엔티티/리포지토리를 만들어줘.
```
