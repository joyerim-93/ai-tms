# AI-TMS (테스트공정관리 포탈)

개발자·현업·QA가 함께 쓰는 테스트 관리 포탈. 기능: ① 대시보드(내 할일) ② 테스트케이스 저장소(+AI 추천) ③ 테스트수행관리(차수) ④ 결함관리.

## 작업 방식
- 단계별로 진행하고 **매 단계 사용자 확인 후** 다음 단계로. 한 번에 다 만들지 말 것.
- 커밋은 작업 단위로 쪼갬. 작업 끝날 때마다 이 파일 갱신.

## 스택
| 영역 | 사용 |
|---|---|
| Backend | Java 21, Spring Boot **3.5.x**, Gradle 8.14.5(wrapper), **MyBatis**(mapper XML) — **JPA 사용 금지** |
| DB | H2 파일 모드 (`backend/data/aitms`, MySQL 모드, git 제외) |
| Frontend | Vue 3 (`<script setup>`), Vite, vue-router 4 |
| 인증 | 미적용. 추후 Spring Security (users 테이블만 미리 설계) |
| AI 추천 | Claude API(RAG) 예정. 지금은 `RecommendationService` 인터페이스 + Noop 구현만 |

> Initializr 기본값이 Boot 4.x라 `build.gradle`에서 3.5.x로 수동 고정했음 (mybatis-spring-boot-starter 3.0.x 호환).

## 구조
```
ai-tms/
├─ backend/                      Spring Boot (com.aitms)
│  └─ src/main/
│     ├─ java/com/aitms/
│     │  ├─ config/              WebConfig(CORS)
│     │  ├─ common/              ApiException, GlobalExceptionHandler, PageResponse, CurrentUser, Priority
│     │  ├─ testcase/            ✅ 테스트케이스 저장소 (Controller/Service/Mapper/DTO)
│     │  ├─ execution/           ✅ 테스트수행관리 (차수 TestCycle*, 수행항목 TestExecution*, 요청 DTO는 ExecutionRequests)
│     │  ├─ project/             프로젝트/멤버 조회
│     │  ├─ recommend/           RecommendationService 인터페이스 + Noop 구현 (AI 추천 자리)
│     │  └─ <도메인>/            controller · service · mapper(인터페이스) · dto — 도메인별 패키지
│     └─ resources/
│        ├─ application.yml
│        ├─ schema.sql           전체 12개 테이블 (CREATE TABLE IF NOT EXISTS, 기동마다 실행)
│        ├─ data.sql             샘플 데이터 (MERGE ... KEY 로 멱등)
│        └─ mapper/<도메인>/*.xml MyBatis 쿼리
└─ frontend/                     Vue 3 + Vite
   └─ src/
      ├─ styles/tokens.css       디자인 토큰 (라이트/다크)
      ├─ styles/base.css         리셋 + 공통 클래스(.card, .btn)
      ├─ composables/useTheme.js 테마 토글
      ├─ composables/useProject.js 전역 선택 프로젝트(헤더 셀렉트, localStorage `aitms-project`) — 프로젝트 종속 화면은 `watch(projectId, load, {immediate:true})`
      ├─ layouts/AppLayout.vue   사이드바 + 헤더
      ├─ api/                    http.js(fetch 래퍼) + 도메인별 API 모듈(testCases.js)
      ├─ constants/labels.js     enum → 한글 표시명, 날짜 포맷
      ├─ components/             KpiCard, StatusBadge, PriorityChip, ProgressBar(결과 누적막대), BaseModal
      ├─ views/                  페이지 (도메인별 폴더: views/testcase/ List·Detail·Form, views/cycle/ List·Detail + TcPickerModal·ExecutionPanel)
      └─ router/index.js
```

## 도메인 모델 (승인됨)
- **users**(login_id, name, email, role `DEV|BIZ|QA|ADMIN`, password nullable)
- **project**, **project_member**(project_id, user_id, project_role)
- **test_case**(중앙 저장소, 프로젝트 비종속: tc_code, title, module, precondition, priority, status `ACTIVE|DEPRECATED`, tags, author_id, version)
- **test_step**(test_case_id, step_no, action, expected_result) — 단계는 행 단위 관리
- **requirement**(project_id, req_code, title, description) — RAG 입력
- **requirement_tc**(requirement_id, test_case_id, source `MANUAL|AI`, score) — 추적/AI 추천 결과
- **test_cycle**(차수: project_id, cycle_no, name, 기간, status `PLANNED|IN_PROGRESS|CLOSED`)
- **test_execution**(cycle_id, test_case_id **UNIQUE**, tc_version, assignee_id, 최종 result `PASS|FAIL|BLOCKED|NOT_RUN`)
- **test_execution_history**(execution_id, result, executed_by, executed_at, comment) — 차수×TC별 모든 수행 기록
- **defect**(project_id, defect_code, severity, priority, status `NEW→OPEN→IN_PROGRESS→RESOLVED→CLOSED|REJECTED`, reporter_id, assignee_id, execution_id nullable)
- **defect_comment**(defect_id, author_id, content, status_from/to)
- 대시보드는 별도 테이블 없이 집계 쿼리.
- 기타 enum: project.status `ACTIVE|CLOSED`, project_role `PM|DEV|BIZ|QA`, priority `HIGH|MEDIUM|LOW`, severity `CRITICAL|MAJOR|MINOR|TRIVIAL`. 모두 VARCHAR + CHECK 제약.

### 스키마/데이터 규칙
- 스키마 변경 시 schema.sql 수정 → IF NOT EXISTS라 기존 DB엔 반영 안 됨 → 개발 중엔 `backend/data/` 삭제 후 재기동.
- data.sql은 `MERGE INTO ... KEY(...)`만 사용(재기동 시 중복 방지). 명시 id로 넣어도 H2가 identity를 자동 조정함(테스트로 확인).
- 샘플 사용자: 1 qa01(QA), 2 dev01, 3 dev02(DEV), 4 biz01(BIZ), 5 admin(ADMIN) / 프로젝트 1 `PRJ-DEMO`. **인증 도입 전까지 로그인 사용자 = id 1(qa01)**.
- 테스트는 `@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:...")`로 인메모리 DB 사용(개발 DB 오염 금지).

## 디자인 토큰 (`frontend/src/styles/tokens.css`)
테마 전환: `<html data-theme="light|dark">`, localStorage 키 `aitms-theme`. index.html 인라인 스크립트로 첫 페인트 전 적용.

| 변수 | 라이트 | 다크 |
|---|---|---|
| `--color-primary` | #1B2A4A (네이비) | 동일 |
| `--color-accent` | #4A9EFF (블루) | 동일 |
| `--bg-app` | #f5f6fa | #1a1a2e |
| `--bg-card` | #ffffff | #1e1e2e |
| `--bg-sidebar` | #1B2A4A | #151526 |
| `--text-primary` / `--text-secondary` / `--text-muted` | #1f2433 / #5b6478 / #8a93a6 | #f1f3f8 / #c3c8d4 / #8b91a3 |
| `--border` | #e3e7ef | #2e2e44 |
| `--shadow-card` | 옅은 네이비 그림자 | 진한 그림자 |
| `--status-pass(-bg)` | 초록 | 초록 |
| `--status-fail(-bg)` | 빨강 | 빨강 |
| `--status-blocked(-bg)` | 회색 | 회색 |
| `--status-notrun(-bg)` | 노랑/주황 | 노랑 |

기타: `--space-1..6`(4/8/12/16/24/32px), `--radius-sm|md|lg`, `--font-size-*`, `--sidebar-width`, `--header-height`.

## 컨벤션
- **색상/간격 하드코딩 금지** → 반드시 `var(--*)`. 새 색이 필요하면 tokens.css에 라이트/다크 둘 다 추가.
- 데스크탑 전용(min-width 1200px), 반응형 고려하지 않음.
- 상태 표시는 `<StatusBadge status="PASS|FAIL|BLOCKED|NOT_RUN" />`, 우선순위는 `<PriorityChip priority="HIGH|MEDIUM|LOW" />`.
- 공통 CSS 클래스(base.css): `.card .card-title .btn(.btn-primary/.btn-danger/.btn-sm) .input .select .textarea .label(.required) .table(tr.clickable) .chip(-high/-medium/-low/-muted) .page-actions .empty .muted .mono .error-text`. 새 화면은 이것부터 재사용.
- Frontend API 호출은 `src/api/<도메인>.js` 경유, 에러는 `e.message`를 화면에 표시.
- 라우트: 목록 `/x`, 등록 `/x/new`, 상세 `/x/:id`, 수정 `/x/:id/edit` (등록/수정은 같은 Form 컴포넌트).
- Backend: 도메인별 패키지, SQL은 mapper XML(`resources/mapper/<도메인>/`)에 작성(어노테이션 SQL 금지), `resultType`은 FQCN 사용(type alias 미사용), DB 컬럼 snake_case → DTO camelCase 자동 매핑.
- 요청 DTO는 record + Bean Validation, 응답/조회 DTO는 Lombok `@Getter @Setter` 클래스.
- 에러: `throw ApiException.notFound(...)/conflict(...)` → `{"message": ...}` 응답. 검증 실패는 400 + 첫 필드 메시지.
- 목록 API는 `PageResponse{items,total,page,size}` 반환, 검색 조건은 `XxxSearch`(page/size/getOffset).
- 로그인 사용자 id는 `CurrentUser.id()`로만 조회 (Security 도입 시 이 한 곳만 교체).
- FK/UNIQUE 위반(DataIntegrityViolation)은 전역 핸들러에서 400 처리.
- 모달은 `BaseModal`(title, width, #footer 슬롯), 상세 편집은 우측 슬라이드 패널(ExecutionPanel 패턴). 오버레이 색은 `--bg-overlay`, 그림자 `--shadow-overlay`.
- 도메인 한 화면에서만 쓰는 하위 컴포넌트는 `views/<도메인>/`에 둠, 여러 곳에서 쓰면 `components/`.

## API 현황
| Method | Path | 설명 |
|---|---|---|
| GET | `/api/test-cases?keyword&module&priority&status&page&size` | 검색+페이징 |
| GET | `/api/test-cases/modules` | 모듈 목록(필터/자동완성) |
| GET | `/api/test-cases/{id}` | 상세(steps 포함) |
| POST / PUT | `/api/test-cases`, `/api/test-cases/{id}` | 등록/수정 (수정 시 version+1, 단계 전체 교체) |
| DELETE | `/api/test-cases/{id}` | 차수 등록 이력 있으면 409 → 폐기(DEPRECATED)로 유도 |

| GET | `/api/projects`, `/api/projects/{id}/members` | 프로젝트 목록 / 담당자 후보 |
| GET | `/api/cycles?projectId` | 차수 목록 + 결과별 집계(totalCount/passCount/failCount/blockedCount/notRunCount) |
| GET / POST / PUT / DELETE | `/api/cycles[/{id}]` | 차수 CRUD (번호 자동 채번, 생성 시 PLANNED, 이력 있으면 삭제 409) |
| GET | `/api/cycles/{id}/executions?result&assigneeId&keyword` | 차수별 수행 항목 |
| POST | `/api/cycles/{id}/executions` `{testCaseIds, assigneeId}` | TC 등록 (ACTIVE·미등록만, `{added}` 반환) |
| PUT | `/api/cycles/{id}/executions/assignee` `{executionIds, assigneeId}` | 담당자 일괄 지정 (null=해제) |
| DELETE | `/api/cycles/{id}/executions/{executionId}` | 차수에서 제외 (이력 있으면 409) |
| GET | `/api/executions/{id}`, `/api/executions/{id}/history` | 수행 항목 / 이력(최신순) |
| POST | `/api/executions/{id}/results` `{result, comment}` | 결과 입력 → 최종결과 갱신 + 이력 추가 |

TC 코드는 `tc_code_seq` 시퀀스로 `TC-00001` 형식 채번.

### 테스트수행 규칙
- 결과 입력 시 수행자=CurrentUser, 차수가 PLANNED면 IN_PROGRESS로 자동 전환.
- CLOSED 차수는 TC 등록·담당 지정·제외·결과 입력 모두 409 (상태를 되돌리면 가능).
- 진행률 = (전체 − 미수행) / 전체 (`labels.js progressRate`).
- `test_execution.tc_version`(등록 시점) ≠ 현재 TC 버전이면 화면에 'TC 변경됨' 표시. 단계는 현재 버전 기준으로 보여줌(스냅샷 미보관).
- API prefix `/api`. enum은 DB에 문자열로 저장.
- 커밋 메시지: `type(scope): 한글 요약` (feat/fix/chore/refactor/docs).

## 실행
```bash
# backend (JDK 21 필요 — 시스템 기본이 24면 JAVA_HOME 지정)
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew bootRun     # http://localhost:8080
./gradlew build                                                  # 빌드 + 테스트
# H2 콘솔: http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:file:./data/aitms, sa / 빈 비밀번호)

# frontend
cd frontend
npm install
npm run dev      # http://localhost:5173  (/api → 8080 프록시)
npm run build
```
