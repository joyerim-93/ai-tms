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
│     │     ├─ testcase/         ✅ 테스트케이스 (+ 추천 출처/검토, 폴더 TestCaseFolder*, 다른 프로젝트에서 가져오기)
│     │     ├─ recommend/        RecommendationService + RuleBasedRecommendationService(규칙기반 3-1), RuleCatalog
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
      │                          PriorityChip, LabelChip(labels 맵 기반 칩), ProgressBar(결과 누적막대), BaseModal, FolderTree(재귀), TestCaseKeyBadge(Key 뱃지), ResultSelect(결과 뱃지 클릭 → 드롭다운 즉시 저장), DatasetTable(스프레드시트형 데이터셋 — 편집/읽기전용·강조 행)
      ├─ utils/folders.js        폴더 트리 평면화(flattenFolders)·들여쓰기 라벨
      ├─ utils/params.js         파라미터화 {변수} 치환(substitute)·변수 추출·셀 값 파싱·braced()
      ├─ views/                  페이지 (도메인별 폴더: views/testcase/ List(좌 FolderTree/우 목록)·Detail·Form + RepoTabs(+새 프로젝트)·NewProjectModal·ImportTestCaseModal, views/cycle/ List·Detail + TcPickerModal·ExecutionPanel, views/defect/ List·Detail·Form)
      └─ router/index.js
```

## 도메인 모델 (승인됨)
- **users**(login_id, name, email, role `DEV|BIZ|QA|ADMIN`, password nullable)
- **project**, **project_member**(project_id, user_id, project_role)
- **test_case_folder**(project_id, parent_folder_id 자기참조=중첩, name, sort_order) — 트리는 서비스에서 parent_folder_id로 조립
- **test_case**(**프로젝트 소유** project_id NOT NULL, folder_id NULL=미분류, tc_code, title, module, precondition, priority, status `ACTIVE|DEPRECATED`, tags, author_id(NULL=시스템/AI), version,
  source `MANUAL|RULE|RAG|LLM`, technique `BOUNDARY_VALUE|EQUIVALENCE_PARTITION|DECISION_TABLE|EXPLORATORY`, review_status `DRAFT|APPROVED|REJECTED`, origin_project_id(RAG/가져오기 원본), reviewed_by/at)
- **test_step**(test_case_id, step_no, action, expected_result) — 단계는 행 단위 관리
- **requirement**(project_id, req_code `REQ-001`, title, description=원문, source `MANUAL|PMS`, priority) — AI 추천 입력
- **atomic_requirement**(requirement_id, atomic_text, type `AMOUNT_RANGE|RATE_RANGE|PERIOD_CONDITION|BOOLEAN_FLAG`, min/max_value, unit, conditions JSON) — AI 에이전트가 원문을 분해한 결과
- **rule_catalog**(requirement_type, technique, template=사람용 설명, **generator**=추천 생성 규칙 JSON) — 규칙기반 추천
- **test_case_dataset**(test_case_id, row_label, param_values JSON, expected_result_override, sort_order) — 파라미터화 TC(test_case.is_parameterized) 데이터 행
- **test_execution.dataset_id** — 파라미터화 TC는 데이터셋 행마다 실행 항목 1건 (NULL = TC 단위)
- **test_case_requirement_link**(test_case_id, atomic_requirement_id, UNIQUE, 양쪽 ON DELETE CASCADE) — TC ↔ 원자 요구사항 **다대다**(Zephyr Traceability). test_case.atomic_requirement_id는 삭제됨.
- 추적: requirement → atomic_requirement ⇄ test_case_requirement_link ⇄ test_case.
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
- 코드 채번(TC Key `TC-101`~, REQ-001, DF-0001)은 모두 `MAX(번호)+1` 방식 → 샘플의 명시 코드와 충돌 없음. TC Key는 `tc_code`(전역 순번, 'TC-숫자'만 집계, 최소 101), 화면 표기 **Key**.
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
| GET | `/api/test-cases/modules?projectId` | 모듈 목록(자동완성, projectId 없으면 전체) |
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

| GET | `/api/test-cases?projectId&excludeProjectId&folderId&unfiled&...` | folderId는 하위 폴더 포함, unfiled=미분류만, projectId 없으면 전체(가져오기 검색) |
| POST | `/api/test-cases/import` `{projectId, testCaseIds, folderId}` | 다른 프로젝트 APPROVED·ACTIVE TC를 새 row로 복제 |
| GET / POST | `/api/projects/{id}/folders` `{name, parentFolderId}` | 폴더 트리(roots/totalCount/unfiledCount) / 생성(형제 중 마지막, 같은 이름 409) |
| POST | `/api/projects` `{code, name, description, startDate, endDate}` | 프로젝트 등록 (코드 대문자, 등록자 PM 자동 참여) — 화면은 테스트케이스 탭에서만 |
| GET / POST / PUT / DELETE | `/api/test-cases/{id}/datasets[/{rowId}]` `{rowLabel, paramValues:{}, expectedResultOverride}` | 데이터셋 행 CRUD (행 단위 — 실행 항목이 참조, 실행된 행 삭제 409) |
| PUT | `/api/test-cases/{id}/requirements` `{atomicRequirementIds}` | 연결 요구사항 교체 ('연결된 요구사항' 탭) |
| GET | `/api/test-cases/{id}/runs` | 실행 이력 (모든 차수, 시간 역순, 데이터 행 포함) |
| PATCH | `/api/test-cases/{id}/review` `{reviewStatus}` | AI 추천 TC 승인/반려 (검토자=CurrentUser) |
| GET / POST | `/api/requirements?projectId`, `/api/requirements` | 요구사항 목록(원자·연결TC 수) / 등록(MANUAL, REQ-### 채번) |
| GET | `/api/requirements/{id}` | 상세 + 원자 요구사항 + 원자별 커버 TC(`atomics[].testCases`, Traceability) |
| GET | `/api/requirements/atomics?projectId` | 프로젝트 원자 요구사항 전체(원문 코드·제목 포함) — TC 폼 선택용 |
| POST | `/api/requirements/{id}/recommend` | 추천 실행 → DRAFT TC 저장, `{created[], skipped, warnings[]}` (현재 규칙기반) |
| GET | `/api/rule-catalog` | 규칙 카탈로그 |

### 프로젝트 소유 · 중앙관리 규칙 (v4)
- TC는 project_id로 각 프로젝트가 **소유**(Zephyr 구조). 같은 row를 여러 프로젝트가 공유하는 N:N 공유는 **하지 않음**.
- '중앙관리'는 검색/추천 레이어에서: ① AI 추천(RAG)은 project_id 제한 없이 **전체 프로젝트의 APPROVED TC**를 검색 대상으로 ② '다른 프로젝트에서 가져오기' = 새 row로 복제 + `origin_project_id`=원본 프로젝트(요구사항 연결은 복사 안 함, 복제본은 MANUAL·APPROVED).
- 폴더는 같은 프로젝트 안에서만(교차 프로젝트 폴더 지정 400). TC 수정 시 프로젝트 이동 불가, 폴더 이동은 가능.
- 차수에는 **같은 프로젝트의** ACTIVE·APPROVED TC만 등록.
- 폴더 선택 상태는 URL `?folder=all|unfiled|<id>`로 유지(상세→목록 복귀 시 같은 폴더).
- **프로젝트 필터링 원칙(v4-4):** 목록·집계 API는 모두 `projectId` 쿼리(또는 차수/수행항목/이슈 id처럼 이미 프로젝트가 정해진 경로)로 조회. 예외는 설계상 전체 대상인 것만 — 규칙 카탈로그, '다른 프로젝트에서 가져오기' 검색(`excludeProjectId`), RAG 검색(추후).
  경로는 문서 초안과 다르게 기존 `/api/dashboard`, `/api/cycles` 유지(사용자 확인).
- 프로젝트 전환 시 TC/차수/이슈의 상세·수정 화면(`params.id`)에 있으면 해당 목록으로 이동.

### 요구사항 ↔ TC 다대다 규칙 (v4-3)
- TC 등록/수정 body의 `atomicRequirementIds`로 링크 **전체 교체**(중복 id 제거). **같은 프로젝트의 원자 요구사항만** 연결(아니면 400).
- TC 상세 `requirements[]`(AtomicRequirementRef), 목록 `requirementCount`. 요구사항 목록 `testCaseCount`(중복 제거 TC 수)·`coveredAtomicCount`(TC 1건 이상 연결된 원자 수).
- 다른 프로젝트에서 가져온 TC·RAG 추천 TC의 참고 출처는 링크가 아니라 `origin_project_id`로 표시 (샘플 TC 8·9는 링크 없음).
- 화면: TC 폼 '검증하는 요구사항'(RequirementLinkPicker, 원문별 그룹 체크), TC 상세 목록(→ `/test-cases/requirements?req=<id>`로 펼침), 요구사항 탭 원자별 커버 TC + '미커버' 표시, 커버리지 M/N.

### 파라미터화 TC 규칙 (docs/08·09)
- 단계의 수행 절차/기대 결과에 `{변수}` 사용. 데이터셋 행 `paramValues`로 치환, `{expected}`는 행의 `expectedResultOverride`. **치환은 프론트**(백엔드는 원본 텍스트 + JSON만 — `@JsonRawValue`로 객체 그대로 내려줌).
- 변수명: 문자·숫자·_ (첫 글자 숫자 불가), 값은 스칼라만.
- 차수 등록: 파라미터화 TC는 데이터셋 행마다 실행 항목 생성, 재등록 시 새로 추가된 데이터 행만 생성. 데이터 행이 없으면 TC 단위 1건.
- 실행된 데이터 행은 삭제 불가(409). 다른 프로젝트에서 가져오기 시 데이터셋도 복제.
- 샘플: TC-114(가입금액 경계값, 데이터 4행)가 TC-101~104를 대체 → 101~104는 DEPRECATED.
- 화면(Zephyr 정보 배치, 톤은 v3 유지): TC 목록 `Key | 제목(🔢 N) | 폴더 | 기법 | 출처 | 상태(검토) | 데이터셋`.
  TC 상세 탭(`?tab=`): 개요 | 테스트 스크립트(데이터 행 선택 시 치환 미리보기) | 데이터셋(DatasetTable 셀 편집, 포커스 아웃 저장, 행/변수 열 추가·삭제) | 실행 이력 | 연결된 요구사항(추가/제거).
  폼: '파라미터화' 토글 + 단계에서 찾은 변수 표시, 새로 켜고 저장하면 데이터셋 탭으로 이동.
- 차수 상세(Zephyr "Test Cycle" 배치): `TC Key | 제목 | 결과(ResultSelect 인라인, 코멘트 없이 결과만 기록) | 담당자 | 실행일시 | 코멘트(최근)`.
  파라미터화 TC는 상위 행(🔢 N, "N개 행 중 M개 성공 · …" 요약, 행별 색 미니 막대, 접기) + 데이터셋 하위 행(들여쓰기, 변수=값 칩). 상위 체크박스 = 하위 전체 선택.
  하위 행 클릭 → 결과 입력 패널: 단계·기대결과를 이 행 값으로 치환 + DatasetTable(읽기 전용, 현재 행 강조). 진행률·결과 건수는 데이터 행을 각각 1건으로 집계.
- Vue 템플릿 `{{ }}` 안에서 `` `{${v}}` `` 금지(`}}`가 보간을 닫음) → `braced(v)` 사용.

### 규칙기반 추천 (3-1)
- `RuleBasedRecommendationService`가 원자 요구사항 유형별 rule_catalog.generator로 **파라미터화 TC 1개 + 데이터셋 N행** 후보 생성 → `RequirementService.recommend`가 `TestCaseService.createDraft`로 저장
  (source RULE, review DRAFT, 미분류 폴더, 작성자 없음, 원자 요구사항 링크, 데이터셋).
- generator JSON: `title`, `steps[{action, expected}]`, `rows[{label, value|flag, expected}]` 또는 `rowsFrom: "options"`(+`label`, `expected`), `step`(경계 간격).
  `[[text]] [[unit]] [[flag]] [[bonus]]` = 생성 시 원자 요구사항 값으로 치환 / `{value} {option} {flag} {expected}` = TC에 남는 데이터셋 변수. value 식: `min`, `max`, `min±n`, `max±n` (×step).
- 유형별: 금액 범위 → 경계값 6행(step 1) / 비율 범위 → 경계값 4행(step 0.01) / 기간 조건 → 선택지별 결정 테이블(conditions.options, *_map → {mapped}) / 여부 플래그 → 동등 분할 2행(*bonus* → 가산값).
- 재실행 시 같은 프로젝트·원자 요구사항·제목·출처의 ACTIVE TC가 있으면 건너뜀(skipped). 범위·조건 값이 없거나 규칙이 없으면 warnings.
- 단위 표기: KRW→원, percent→%, year→년. 숫자 param은 정수면 Long, 소수면 BigDecimal.
- RAG·LLM(3-2, 3-3)은 ai-agent 연동 시 RecommendationService 구현을 합성해서 추가.

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
- 총 테스트케이스 = **현재 프로젝트** ACTIVE TC 수, 보조: 최근 7일 등록 수.
- 수행 통과율 = 현재 차수 PASS / 수행완료(미수행 제외), 보조: 직전 차수 대비 %p. 현재 차수 = IN_PROGRESS 최신 → 없으면 PLANNED 최신.
- 열린 이슈 = NEW/OPEN/IN_PROGRESS, 보조: 최근 7일 등록 수. 활성 테스트 차수 = CLOSED 아닌 차수, 보조: 진행중 수.
- 최근 등록된 이슈 = 프로젝트 이슈 최신 5건(defect 검색 API 재사용). 테스트 차수별 진행률 = 진행중→계획→종료 순 3개, **진행률 % = 통과 / 전체**(레퍼런스 기준, `labels.js passRate`) — 차수 화면의 진행률(수행완료/전체, `progressRate`)과 다름에 주의.
- `/api/dashboard`는 내 할일 필드(myExecutions 등)도 계속 내려줌 — v3 화면에서는 미사용.

## 진행 현황
- ✅ 골격 / 스키마 / ② TC 저장소 / ③ 테스트수행 / ④ 결함 / ① 대시보드
- ✅ docs/00-SKELETON.md 병합 (요구사항/원자요구사항/규칙카탈로그, TC 출처·기법·검토) — 단, 문서의 JPA·test_round 명칭·Pinia·문자열 담당자는 채택하지 않음(MyBatis, test_cycle, users FK 유지)
- ✅ v4-1 프로젝트 전역화 (Pinia projectStore + 헤더 ProjectSelector, pill ProjectTabs 제거)
- ✅ v4-2 TC 프로젝트 소유 + 폴더 트리 + 프로젝트 등록 + 다른 프로젝트에서 가져오기
- ✅ v4-3 TC ↔ 요구사항 다대다 + Traceability
- ✅ v4-4 나머지 화면 프로젝트 필터링 (대시보드 TC 집계·모듈 목록 프로젝트 기준, TC 상세 전환 처리) — **v4 완료**
- ✅ 디자인 v3 전면 교체 (상단 탭 + ProjectTabs, theme.css 토큰, StatusBadge 공용화, 대시보드 카드 3종)
- 🔄 파라미터화 TC (docs/09): ✅ A 스키마·백엔드 / ✅ B TC 목록·상세 탭·DatasetTable / ✅ C 차수 상세 테이블 — **완료** 
- ✅ 규칙기반 추천(3-1) 연결 — 파라미터화 TC + 데이터셋 DRAFT 생성
- ⏳ 이후 후보: ai-agent(FastAPI /decompose) + AiAgentClient 뼈대, 다크모드 값, 테스트케이스/수행/이슈 화면 상단 StatCard, Spring Security 로그인(CurrentUser 교체), 요구사항 관리 + AI 추천(Claude API, RecommendationService 구현), TC 단계 스냅샷, 프로젝트/사용자 관리 화면
