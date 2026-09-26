# AI-TMS (테스트공정관리 포탈)

개발자·현업·QA가 함께 쓰는 테스트 관리 포탈. 기능: ① 대시보드(내 할일) ② 테스트케이스 저장소(+AI 추천) ③ 테스트수행관리(차수) ④ 이슈관리(결함 — 코드/DB는 defect, 화면 명칭은 '이슈').

## 작업 방식
- 단계별로 진행하고 **매 단계 사용자 확인 후** 다음 단계로. 한 번에 다 만들지 말 것.
- 커밋은 작업 단위로 쪼갬. 작업 끝날 때마다 이 파일 갱신.

## 스택
| 영역 | 사용 |
|---|---|
| Backend | Java 21, Spring Boot **3.5.x**, Gradle 8.14.5(wrapper), **MyBatis**(mapper XML) — **JPA 사용 금지** (docs/03-SKELETON-v2.md 기준) |
| DB | H2 파일 모드 (`backend/data/aitms`, MySQL 모드, git 제외) |
| Frontend | Vue 3 (`<script setup>`), Vite, vue-router 4, Pinia |
| 인증 | 미적용. 추후 Spring Security (users 테이블만 미리 설계) |
| AI 추천 | Claude API(RAG) 예정. 지금은 `RecommendationService` 인터페이스 + Noop 구현만 |

> Initializr 기본값이 Boot 4.x라 `build.gradle`에서 3.5.x로 수동 고정했음 (mybatis-spring-boot-starter 3.0.x 호환).

## 구조
```
ai-tms/
├─ backend/                      Spring Boot (com.aitms)
│  └─ src/main/
│     ├─ java/com/aitms/
│     │  ├─ AiTmsApplication.java
│     │  ├─ config/              WebConfig(CORS)  (ai-agent 호출용 HTTP 클라이언트 설정 예정)
│     │  ├─ common/              ApiException, GlobalExceptionHandler, PageResponse, CurrentUser, Priority
│     │  └─ domain/              도메인별 패키지: VO/DTO · XxxMapper(@Mapper) · XxxService · XxxController
│     │     ├─ project/          프로젝트/멤버 조회
│     │     ├─ requirement/      요구사항 원문 + 원자 요구사항 조회/등록, AI 추천 트리거
│     │     ├─ testcase/         ✅ 테스트케이스 저장소 (+ 추천 출처/검토)
│     │     ├─ recommend/        RecommendationService 인터페이스 + Noop 구현, RuleCatalog(규칙 카탈로그)
│     │     ├─ execution/        ✅ 테스트수행관리 (차수 TestCycle*, 수행항목 TestExecution*) — 문서의 testrun/TestRound에 해당
│     │     ├─ defect/           ✅ 결함관리 (DefectStatus에 상태 전이 규칙)
│     │     └─ dashboard/        ✅ 대시보드 요약
│     └─ resources/
│        ├─ application.yml      mybatis.mapper-locations = classpath:mapper/*.xml
│        ├─ schema.sql           전체 13개 테이블 (CREATE TABLE IF NOT EXISTS, 기동마다 실행)
│        ├─ data.sql             KB 적금 시나리오 샘플 (INSERT IGNORE — 최초 1회만 반영)
│        └─ mapper/XxxMapper.xml MyBatis 쿼리 — Mapper 인터페이스 1개당 XML 1개, 한 폴더(namespace = com.aitms.domain.<도메인>.XxxMapper)
└─ frontend/                     Vue 3 + Vite
   └─ src/
      ├─ styles/theme.css        디자인 토큰 v3 (docs/04-DESIGN-v3.md) — 라이트만, 다크는 추후 같은 변수명으로
      ├─ styles/base.css         리셋 + 공통 클래스(.card, .btn)
      ├─ stores/projectStore.js  Pinia 전역 프로젝트 컨텍스트: projects, currentProjectId(localStorage `aitms-project`), currentProject, loadProjects(), selectProject()
      ├─ layouts/AppLayout.vue   AppHeader + 가운데 정렬 콘텐츠(max 1120px). 기동 시 loadProjects(), 프로젝트 전환 시 차수/이슈 상세 화면이면 목록으로 이동
      ├─ api/                    http.js(fetch 래퍼) + 도메인별 API 모듈(testCases.js)
      ├─ constants/labels.js     enum → 한글 표시명, 날짜 포맷
      ├─ components/             AppHeader(메뉴 + 우측 ProjectSelector), ProjectSelector(전환 전용 드롭다운), StatCard, IssueListCard, TestRoundProgressCard, StatusBadge(공용 상태 뱃지),
      │                          PriorityChip, LabelChip(labels 맵 기반 칩), ProgressBar(결과 누적막대), BaseModal
      ├─ views/                  페이지 (도메인별 폴더: views/testcase/ List·Detail·Form, views/cycle/ List·Detail + TcPickerModal·ExecutionPanel, views/defect/ List·Detail·Form)
      └─ router/index.js
```

## 도메인 모델 (승인됨)
- **users**(login_id, name, email, role `DEV|BIZ|QA|ADMIN`, password nullable)
- **project**, **project_member**(project_id, user_id, project_role)
- **test_case**(중앙 저장소, 프로젝트 비종속: tc_code, title, module, precondition, priority, status `ACTIVE|DEPRECATED`, tags, author_id(NULL=시스템/AI), version,
  source `MANUAL|RULE|RAG|LLM`, technique `BOUNDARY_VALUE|EQUIVALENCE_PARTITION|DECISION_TABLE|EXPLORATORY`, review_status `DRAFT|APPROVED|REJECTED`, atomic_requirement_id, origin_project_id(RAG 원본), reviewed_by/at)
- **test_step**(test_case_id, step_no, action, expected_result) — 단계는 행 단위 관리
- **requirement**(project_id, req_code `REQ-001`, title, description=원문, source `MANUAL|PMS`, priority) — AI 추천 입력
- **atomic_requirement**(requirement_id, atomic_text, type `AMOUNT_RANGE|RATE_RANGE|PERIOD_CONDITION|BOOLEAN_FLAG`, min/max_value, unit, conditions JSON) — AI 에이전트가 원문을 분해한 결과
- **rule_catalog**(requirement_type, technique, template `{min}/{max}` 치환) — 규칙기반 추천
- 추적: requirement → atomic_requirement → test_case(atomic_requirement_id). (구 requirement_tc 제거)
- **test_cycle**(차수: project_id, cycle_no, name, 기간, status `PLANNED|IN_PROGRESS|CLOSED`)
- **test_execution**(cycle_id, test_case_id **UNIQUE**, tc_version, assignee_id, 최종 result `PASS|FAIL|BLOCKED|NOT_RUN`)
- **test_execution_history**(execution_id, result, executed_by, executed_at, comment) — 차수×TC별 모든 수행 기록
- **defect**(project_id, defect_code, severity, priority, status `NEW→OPEN→IN_PROGRESS→RESOLVED→CLOSED|REJECTED`, reporter_id, assignee_id, execution_id nullable)
- **defect_comment**(defect_id, author_id, content, status_from/to)
- 대시보드는 별도 테이블 없이 집계 쿼리.
- 기타 enum: project.status `ACTIVE|CLOSED`, project_role `PM|DEV|BIZ|QA`, priority `HIGH|MEDIUM|LOW`, severity `CRITICAL|MAJOR|MINOR|TRIVIAL`. 모두 VARCHAR + CHECK 제약.

### 스키마/데이터 규칙
- 스키마 변경 시 schema.sql 수정 → IF NOT EXISTS라 기존 DB엔 반영 안 됨 → 개발 중엔 `backend/data/` 삭제 후 재기동.
- data.sql은 `INSERT IGNORE INTO`만 사용 — 재기동 시 중복·덮어쓰기 없음(사용자가 바꾼 샘플 데이터 유지). 명시 id로 넣어도 H2가 identity를 자동 조정함(테스트로 확인).
- 코드 채번(TC-00001, REQ-001, DF-0001)은 모두 `MAX(번호)+1` 방식 → 샘플의 명시 코드와 충돌 없음.
- 샘플(docs/02-sample-data.sql 변환): 사용자 1 qa.kim 김큐에이(QA), 2 dev.park 박개발(DEV), 3 qa.lee 이큐에이(QA), 4 biz.choi(BIZ), 5 admin / 프로젝트 1 `KB-SAVING` KB 적금 통장 신설, 2 `KB-SWITCH` 갈아타기(과거, RAG 원본) / 요구사항 REQ-001 + 원자 4 + 규칙 4 / TC 11(RULE·RAG·LLM, DRAFT 5) / 1차 통합테스트(PASS4·FAIL1·BLOCKED1·미수행3) / 결함 DF-0001. **인증 도입 전까지 로그인 사용자 = id 1(qa.kim)**.
- 서비스 테스트는 샘플과 분리하려고 테스트 전용 프로젝트(id 99/98)를 `@BeforeEach`에서 넣어 사용.
- 테스트는 `@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:...")`로 인메모리 DB 사용(개발 DB 오염 금지).

## 디자인 v3 (docs/04-DESIGN-v3.md, docs/dashboard-reference.jpg)
- **레이아웃:** 좌측 사이드바 폐기 → 상단 `AppHeader`(로고 AI-TMS + 탭 4개: 대시보드/테스트케이스/테스트 수행/이슈관리, 활성 탭 `--accent` 밑줄) + 헤더 우측 `ProjectSelector` 드롭다운(v4: **전환만**, 등록은 테스트케이스 화면에서만). 페이지 제목(h1) 없음.
- **라이트 모드만.** 테마 토글·useTheme 제거. 다크는 theme.css 하단 `[data-theme='dark']`에 **같은 변수명**으로 값만 추가.

### 토큰 (`frontend/src/styles/theme.css`)
1) 문서 정의(그대로 유지): `--surface-page #F5F6FA`, `--surface-card #FFF`, `--border #E7E8EF`, `--text-primary #1F2430`, `--text-secondary #6B7280`, `--text-muted #9CA3AF`, `--accent #4F5FF0`, `--accent-soft #EEF0FE`,
   `--badge-{open|progress|resolved|closed}-{bg|text}`(이슈: 빨강/노랑/초록/회색), `--result-{success|fail|block|notrun}-{bg|text}`(결과: 초록/빨강/**노랑**/**회색**), `--radius-card 12px`, `--shadow-card`
2) 확장(문서에 없는 UI용): `--surface-hover`, `--surface-muted`(pill 트랙), `--accent-hover`, `--on-accent`, `--overlay`, `--shadow-overlay`, `--priority-{high|medium|low}-{bg|text}`(=fail/block/notrun)
3) 레이아웃: `--header-height`, `--content-max-width`, `--space-1..6`(4~32px), `--radius-sm|md|pill`, `--font-size-xs..xl`, `--font-size-stat`(32px), `--font-sans`, `--font-mono`

### 상태 뱃지 (`StatusBadge.vue` 하나로 공용)
| status | 표시 | 톤 |
|---|---|---|
| PASS / FAIL / BLOCKED / NOT_RUN | 성공 / 실패 / Block / 미수행 | result-success / fail / block / notrun |
| NEW, OPEN / IN_PROGRESS / RESOLVED / CLOSED, REJECTED | 신규·열림 / 진행중 / 해결됨 / 종료·반려 | badge-open / progress / resolved / closed |
| PLANNED (차수) | 계획 | badge-closed (차수 IN_PROGRESS·CLOSED는 이슈와 같은 톤·표시명) |
표시명은 `labels.js`(RESULT, DEFECT_STATUS, CYCLE_STATUS)에서 가져옴 — 색·이름을 바꿀 땐 StatusBadge의 TONE / labels.js 한 곳만 수정.

## 컨벤션
- **색상/간격 하드코딩 금지** → 반드시 `var(--*)`. 새 색이 필요하면 theme.css의 '확장 토큰'에 추가(다크 추가 시 같은 이름으로 재정의).
- 데스크탑 전용(min-width 1200px), 반응형 고려하지 않음.
- 테스트 결과·이슈 상태·차수 상태는 모두 `<StatusBadge :status />`, 우선순위는 `<PriorityChip />`, 심각도·출처·검토상태는 `<LabelChip :map :value />`.
- 요약 카드는 `<StatCard title value unit sub />` 재사용 (다른 화면 상단 요약에도 사용 예정).
- 공통 CSS 클래스(base.css): `.card .card-title .btn(.btn-primary/.btn-danger/.btn-sm) .input .select .textarea .label(.required) .table(tr.clickable) .chip(-high/-medium/-low/-muted) .page-actions .empty .muted .mono .error-text`. 새 화면은 이것부터 재사용.
- Frontend API 호출은 `src/api/<도메인>.js` 경유, 에러는 `e.message`를 화면에 표시.
- 프로젝트 종속 화면: `const { currentProjectId: projectId } = storeToRefs(useProjectStore())` + `watch(projectId, load, { immediate: true })`. 선택 변경은 `projectStore.selectProject(id)`로만.
- 라우트: 목록 `/x`, 등록 `/x/new`, 상세 `/x/:id`, 수정 `/x/:id/edit` (등록/수정은 같은 Form 컴포넌트).
- Backend: 도메인별 패키지, SQL은 mapper XML(`resources/mapper/XxxMapper.xml`)에 작성(어노테이션 SQL 금지), `resultType`은 FQCN 사용(type alias 미사용), DB 컬럼 snake_case → DTO camelCase 자동 매핑.
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

| GET | `/api/defects?projectId\|executionId&status&unresolved&severity&assigneeId&keyword&page&size` | 결함 검색 (projectId·executionId 중 하나 필수) |
| GET / POST / PUT | `/api/defects[/{id}]` | 결함 상세/등록/수정 (응답에 `nextStatuses`, 삭제 API 없음 → REJECTED) |
| POST | `/api/defects/{id}/status` `{status, comment}` | 상태 전이 (규칙 위반 409) + 이력 기록 |
| GET / POST | `/api/defects/{id}/comments` | 코멘트·상태이력 타임라인 / 코멘트 추가 |
| GET | `/api/dashboard?projectId` | 대시보드 KPI + 내 미수행 TC·내 담당 결함 상위 10건 |

| PATCH | `/api/test-cases/{id}/review` `{reviewStatus}` | AI 추천 TC 승인/반려 (검토자=CurrentUser) |
| GET / POST | `/api/requirements?projectId`, `/api/requirements` | 요구사항 목록(원자·연결TC 수) / 등록(MANUAL, REQ-### 채번) |
| GET | `/api/requirements/{id}` | 상세 + 원자 요구사항 |
| POST | `/api/requirements/{id}/recommend` | AI 추천 트리거 → `TcRecommendation[]` (현재 Noop=빈 배열) |
| GET | `/api/rule-catalog` | 규칙 카탈로그 |

### AI 추천/검토 규칙
- 흐름: 요구사항 원문 → 원자 요구사항 분해 → 타입별 규칙(RULE)/과거 프로젝트 유사 TC(RAG)/신규 생성(LLM) → `DRAFT` TC → 사람이 승인/반려.
- 화면에서 직접 만든 TC = source MANUAL + APPROVED (검토 대상 아님). 출처·검토상태는 TC 수정 API로 못 바꿈.
- **차수 등록은 ACTIVE + APPROVED TC만** (DRAFT/REJECTED는 제외).
- 에이전트(LangGraph/Claude API) 연동 시 `RecommendationService` 구현체만 교체. `NoopRecommendationService`는 그때 제거하거나 `@ConditionalOnMissingBean`.

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

### 결함관리 규칙
- 코드: 프로젝트 내 `DF-0001` 순번. 등록 시 상태 NEW, 보고자 = CurrentUser.
- 상태 흐름: NEW→OPEN→IN_PROGRESS→RESOLVED→CLOSED, NEW/OPEN→REJECTED, RESOLVED/CLOSED/REJECTED→OPEN(재오픈). 규칙은 `DefectStatus.next()` 한 곳에만 정의 — 화면은 응답의 `nextStatuses`로 버튼 생성.
- 미해결 = NEW/OPEN/IN_PROGRESS (`DefectStatus.UNRESOLVED`, 대시보드 지표에도 동일 기준 사용).
- 상태 변경·코멘트 모두 `defect_comment`에 저장 (statusFrom/To 있으면 상태 변경 이력).
- 수행 항목 연결: 결과 패널(FAIL/BLOCKED)의 '결함 등록' → `/defects/new?executionId=` 로 진입, 제목·본문 템플릿 자동 채움. 다른 프로젝트 수행 항목 연결은 400.
- 표시명/칩 색: `labels.js`의 `DEFECT_STATUS`, `SEVERITY` (`{label, chip}`) + `<LabelChip :map :value>`.

### 대시보드 기준 (헤더 선택 프로젝트)
- 총 테스트케이스 = ACTIVE TC 수(저장소는 프로젝트 비종속 → 전체), 보조: 최근 7일 등록 수.
- 수행 통과율 = 현재 차수 PASS / 수행완료(미수행 제외), 보조: 직전 차수 대비 %p. 현재 차수 = IN_PROGRESS 최신 → 없으면 PLANNED 최신.
- 열린 이슈 = NEW/OPEN/IN_PROGRESS, 보조: 최근 7일 등록 수. 활성 테스트 차수 = CLOSED 아닌 차수, 보조: 진행중 수.
- 최근 등록된 이슈 = 프로젝트 이슈 최신 5건(defect 검색 API 재사용). 테스트 차수별 진행률 = 진행중→계획→종료 순 3개, **진행률 % = 통과 / 전체**(레퍼런스 기준, `labels.js passRate`) — 차수 화면의 진행률(수행완료/전체, `progressRate`)과 다름에 주의.
- `/api/dashboard`는 내 할일 필드(myExecutions 등)도 계속 내려줌 — v3 화면에서는 미사용.

## 진행 현황
- ✅ 골격 / 스키마 / ② TC 저장소 / ③ 테스트수행 / ④ 결함 / ① 대시보드
- ✅ docs/00-SKELETON.md 병합 (요구사항/원자요구사항/규칙카탈로그, TC 출처·기법·검토) — 단, 문서의 JPA·test_round 명칭·Pinia·문자열 담당자는 채택하지 않음(MyBatis, test_cycle, users FK 유지)
- ✅ v4-1 프로젝트 전역화 (Pinia projectStore + 헤더 ProjectSelector, pill ProjectTabs 제거)
- ⏳ v4 남은 단계 (docs/06-DESIGN-v4.md): 2) 테스트케이스 폴더 트리 + 프로젝트 등록 → 3) TC-요구사항 N:N(test_case_requirement_link) → 4) 나머지 화면 필터링
- ✅ 디자인 v3 전면 교체 (상단 탭 + ProjectTabs, theme.css 토큰, StatusBadge 공용화, 대시보드 카드 3종)
- ⏳ 이후 후보: ai-agent(FastAPI /decompose) + AiAgentClient 뼈대, 다크모드 값, 테스트케이스/수행/이슈 화면 상단 StatCard, Spring Security 로그인(CurrentUser 교체), 요구사항 관리 + AI 추천(Claude API, RecommendationService 구현), TC 단계 스냅샷, 프로젝트/사용자 관리 화면
