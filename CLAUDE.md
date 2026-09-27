# AI-TMS (테스트공정관리 포탈)

개발자·현업·QA가 함께 쓰는 테스트 관리 포탈. 기능: ① 대시보드(내 할일) ② 테스트케이스 저장소(+AI 추천) ③ 테스트수행관리(차수) ④ 이슈관리(결함 — 코드/DB는 defect, 화면 명칭은 '이슈').

## 저장소
- 원격: `https://github.com/joyerim-93/ai-tms.git` (origin, 브랜치 main)

## 작업 방식
- 단계별로 진행하고 **매 단계 사용자 확인 후** 다음 단계로. 한 번에 다 만들지 말 것.
- 커밋은 작업 단위로 쪼갬. 작업 끝날 때마다 이 파일 갱신. **이 파일을 통째로 덮어쓰지 말고 변경분만 병합**(과거 요약 커밋이 내용을 유실시킨 적 있음).

## 스택
| 영역 | 사용 |
|---|---|
| Backend | Java 21, Spring Boot **3.5.x**, Gradle 8.14.5(wrapper), **MyBatis**(mapper XML) — **JPA 사용 금지** (docs/03-SKELETON-v2.md 기준), Apache POI(엑셀), Anthropic Java SDK(LLM 추천) |
| DB | H2 파일 모드 (`backend/data/aitms`, MySQL 모드, git 제외) |
| Frontend | Vue 3 (`<script setup>`), Vite, vue-router 4, Pinia |
| 인증 | **Spring Security 세션 로그인**(JSESSIONID) + CSRF 쿠키(XSRF-TOKEN→X-XSRF-TOKEN). 상세는 '### 인증' |
| AI 추천 | 3-1 규칙기반 ✅ · 3-2 RAG ✅(키워드 유사도, 다른 프로젝트 APPROVED TC 검색) · 3-3 LLM ✅(Claude API, Anthropic Java SDK) — `RecommendationEngine` 빈 추가만 하면 `CompositeRecommendationService`가 합침 |

> Initializr 기본값이 Boot 4.x라 `build.gradle`에서 3.5.x로 수동 고정했음 (mybatis-spring-boot-starter 3.0.x 호환).

## 구조
```
ai-tms/
├─ backend/                      Spring Boot (com.aitms)
│  └─ src/main/
│     ├─ java/com/aitms/
│     │  ├─ AiTmsApplication.java
│     │  ├─ config/              WebConfig(CORS)  (ai-agent 호출용 HTTP 클라이언트 설정 예정)
│     │  ├─ common/              ApiException, GlobalExceptionHandler, PageResponse, CurrentUser(SecurityContext), Priority
│     │  └─ domain/              도메인별 패키지: VO/DTO · XxxMapper(@Mapper) · XxxService · XxxController
│     │     ├─ user/             User, UserMapper(로그인 조회·초기 비밀번호)
│     │  ├─ security/            SecurityConfig(세션·CSRF·경로 권한), AuthController(/api/auth), AuthUser, AuthUserDetailsService, PasswordBootstrap, CsrfCookieFilter
│     │     ├─ project/          프로젝트/멤버 조회
│     │     ├─ requirement/      요구사항 원문 + 원자 요구사항 조회/등록, AI 추천 트리거
│     │     ├─ testcase/         ✅ 테스트케이스 (+ 추천 출처/검토, 폴더 TestCaseFolder*, 다른 프로젝트에서 가져오기)
│     │     ├─ recommend/        ✅ RecommendationService(@Primary Composite) ← RecommendationEngine들 + RecommendationJobService/Controller/Mapper(잡 상태): RuleBasedRecommendationService(3-1), RagRecommendationService(3-2, TcRetriever→KeywordTcRetriever/TextSimilarity, RagMapper), LlmRecommendationService(3-3, LlmClient→AnthropicLlmClient, LlmProposal), RecommendationResult/TcRecommendation, RuleCatalog(규칙 카탈로그, generator JSON)
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
      ├─ stores/authStore.js     Pinia 로그인 사용자: user{id,loginId,name,role}, currentUserName, loadMe()/login()/logout(). views/LoginView.vue, api/auth.js
      ├─ stores/projectStore.js  Pinia 전역 프로젝트 컨텍스트: projects, currentProjectId(localStorage `aitms-project`), currentProject, loadProjects(), selectProject()
      ├─ layouts/AppLayout.vue   AppHeader + 가운데 정렬 콘텐츠(max 1120px). 기동 시 loadProjects(), 프로젝트 전환 시 차수/이슈 상세 화면이면 목록으로 이동
      ├─ api/                    http.js(fetch 래퍼) + 도메인별 API 모듈(testCases.js)
      ├─ constants/labels.js     enum → 한글 표시명, 날짜 포맷
      ├─ components/             AppHeader(메뉴 + 우측 ProjectSelector), ProjectSelector(전환 전용 드롭다운), StatCard, IssueListCard, TestRoundProgressCard, StatusBadge(공용 상태 뱃지),
      │                          PriorityChip, LabelChip(labels 맵 기반 칩), ProgressBar(결과 누적막대), BaseModal, FolderTree(재귀, 더블클릭/우클릭 이름 수정),
      │                          RecommendationStatusBadge(추천 잡 상태), TestCaseKeyBadge(Key 표기), DatasetTable(변수=열·데이터=행 스프레드시트, 인라인 편집/읽기전용·강조 행), ResultSelect(결과 뱃지 클릭→드롭다운 즉시 기록)
      ├─ utils/folders.js        폴더 트리 평면화(flattenFolders)·들여쓰기 라벨
      ├─ utils/params.js         substitute/extractVariables/parseCell/braced — 파라미터화 {변수} 치환(프론트 담당)
      ├─ views/                  페이지 (도메인별 폴더: views/testcase/ List(좌 FolderTree/우 목록)·Detail·FormModal(등록/수정 팝업)·ExcelUploadModal(엑셀 업로드) + RepoTabs(+새 프로젝트)·NewProjectModal·ImportTestCaseModal, views/cycle/ List·Detail + TcPickerModal·ExecutionPanel, views/defect/ List·Detail·Form)
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
- **rule_catalog**(requirement_type, technique, template, **generator** JSON) — 규칙기반 추천. generator = 제목·단계 템플릿 + 데이터 행 규칙(경계값 min±step / 선택지별 결정 테이블 / 플래그 동등 분할)
- **test_case_dataset**(test_case_id, row_label, param_values JSON, expected_result_override, sort_order) — 파라미터화 TC(test_case.is_parameterized) 데이터 행
- **test_execution.dataset_id** — 파라미터화 TC는 데이터셋 행마다 실행 항목 1건 (NULL = TC 단위)
- **test_case_requirement_link**(test_case_id, atomic_requirement_id, UNIQUE, 양쪽 ON DELETE CASCADE) — TC ↔ 원자 요구사항 **다대다**(Zephyr Traceability). test_case.atomic_requirement_id는 삭제됨.
- 추적: requirement → atomic_requirement ⇄ test_case_requirement_link ⇄ test_case.
- **test_cycle**(차수: project_id, cycle_no, name, 기간, status `PLANNED|IN_PROGRESS|CLOSED`)
- **test_execution**(cycle_id, test_case_id **UNIQUE**, tc_version, assignee_id, 최종 result `PASS|FAIL|BLOCKED|NOT_RUN`)
- **test_execution_history**(execution_id, result, executed_by, executed_at, comment) — 차수×TC별 모든 수행 기록
- **test_execution**.`is_draft`/`draft_result`/`draft_comment` — 임시저장(확정 전) 값. 확정 결과·이력과는 분리, 저장(record) 시 초기화
- **defect**(project_id, defect_code, severity, priority, status `NEW→OPEN→IN_PROGRESS→RESOLVED→CLOSED|REJECTED`, reporter_id, assignee_id, execution_id nullable)
- **defect_comment**(defect_id, author_id, content, status_from/to)
- **test_execution_attachment**(execution_id, file_name, file_path, content_type, file_size, uploaded_by, uploaded_at) / **defect_attachment**(defect_id, …동일) — 증빙 첨부. 파일은 디스크, DB엔 업로드 루트 기준 **상대 경로**만
- 대시보드는 별도 테이블 없이 집계 쿼리.
- 기타 enum: project.status `ACTIVE|CLOSED`, project_role `PM|DEV|BIZ|QA`, priority `HIGH|MEDIUM|LOW`, severity `CRITICAL|MAJOR|MINOR|TRIVIAL`. 모두 VARCHAR + CHECK 제약.

### 인증 (Spring Security, 2026-09-27)
- **세션 기반 로그인**: `POST /api/auth/login {username, password}`(`loginId`도 허용) → 세션 쿠키(HttpOnly, SameSite=Lax, 8h) + `{id, username, displayName, role}`, `GET /api/auth/me`, `POST /api/auth/logout`(204). **계정 테이블은 기존 `users` 하나를 씀**(username=`login_id`, displayName=`name`, password=BCrypt `password`) — 별도 `app_user` 테이블은 만들지 않음(작성자/실행자 등 모든 FK가 users를 참조). 실패는 계정 존재 여부와 무관하게 같은 401 메시지. 비밀번호는 **BCrypt**(`users.password`), 비밀번호 없는 계정(임시 `guest-*` 등)은 로그인 불가. 로그인 시 세션 ID 재발급(세션 고정 방지).
- **회원가입** `POST /api/auth/register {username, password, displayName(display_name도 허용)}` → 201, 기본 역할 QA, 비밀번호 BCrypt. 검증: 아이디 3~50자 `[A-Za-z0-9._-]`, 비밀번호 8~64자, 표시 이름 필수(50자), 중복 아이디 409. **누구나 가입 가능**(로그인 불필요) — 외부 노출 환경에서는 `app.security.registration-enabled: false`. 화면은 가입 직후 자동 로그인.
- **`/api/**` 전부 로그인 필수**(401 `{message}`), `/api/auth/login`·`/api/auth/register`·`/error`만 공개. `/h2-console/**`은 **ADMIN 역할만**. 그 외 역할별 권한(RBAC)은 아직 없음 — 인증만 적용(역할 DEV/BIZ/QA/ADMIN은 principal 에 `ROLE_xxx`로 실려 있음).
- **CSRF**: 서버가 `XSRF-TOKEN` 쿠키를 내려주고 `http.js`가 GET 외 요청에 `X-XSRF-TOKEN` 헤더로 되돌려 보냄(없으면 403).
- **시드 계정**: `3171613` / `3171613`(역할 QA, data.sql에 BCrypt 해시로 저장). 개발용 — 운영에서는 삭제/변경.
- **CORS**: `WebConfig`가 `allowCredentials(true)`(+`Security .cors()`)로 프론트 출처(`app.cors.allowed-origins`)에 쿠키를 허용, 프론트는 `credentials:'include'`. 개발 기본은 Vite 프록시(같은 출처).
- **초기 비밀번호(개발용)**: 기동 시 비밀번호가 없는 사용자에게 `app.security.initial-password`(기본 `aitms1234!`)를 BCrypt 로 설정(`PasswordBootstrap`, WARN 로그). 샘플 계정: `qa.kim`(김큐에이·QA) `dev.park`(박개발·DEV) `qa.lee`(이큐에이·QA) `biz.choi`(BIZ) `admin`(ADMIN). **운영에서는 반드시 변경/제거.** 회원가입·비밀번호 변경·계정관리 화면은 아직 없음.
- `CurrentUser.id()` = 세션 principal(`AuthUser`)의 id. 인증 컨텍스트가 없는 곳(서비스 테스트, 추천 백그라운드 스레드)만 기본 사용자 id 1로 대체. 작성자·실행자·보고자·검토자·코멘트 작성자·프로젝트 등록자(PM)는 모두 로그인 사용자로 서버가 기록 — 화면의 작성자/실행자/보고자는 **읽기 전용 표시**(직접 입력 불가).
- 프론트: `authStore`(user, currentUserName=displayName, loadMe/login/register/logout), `LoginView`·`RegisterView`, CSRF 쿠키가 없으면(로그아웃 직후) `http.js`가 GET 한 번으로 새 토큰을 받은 뒤 전송. 헤더 우측 순서 **프로젝트 선택 → `ThemeToggle` → 사용자명**(`{이름} 님` 클릭 → 로그아웃 드롭다운, `ProjectSelector`와 같은 바깥클릭/Esc 패턴), 라우터 가드(비로그인 → `/login?redirect=`), `http.js`가 401 을 받으면 로그인 화면으로. TC 차수 TC 추가 팝업의 담당자 기본값 = 로그인 사용자(멤버인 경우, 변경 가능).
- (이력) 로그인 도입 전 임시로 '화면에 입력한 이름 → X-User-Name 헤더' 방식(userStore, 이름 입력 팝업, guest 사용자)을 썼으나 로그인으로 대체하며 제거함.

### 스키마/데이터 규칙
- 스키마 변경 시 schema.sql 수정 → IF NOT EXISTS라 기존 DB엔 반영 안 됨 → 개발 중엔 `backend/data/` 삭제 후 재기동.
- data.sql은 `INSERT IGNORE INTO`만 사용 — 재기동 시 중복·덮어쓰기 없음(사용자가 바꾼 샘플 데이터 유지). 명시 id로 넣어도 H2가 identity를 자동 조정함(테스트로 확인).
- 코드 채번(TC Key `TC-101`~, REQ-001, DF-0001)은 모두 `MAX(번호)+1` 방식 → 샘플의 명시 코드와 충돌 없음. TC Key는 `tc_code`(전역 순번, 'TC-숫자'만 집계, 최소 101), 화면 표기 **Key**.
- 샘플(docs/02-sample-data.sql 변환): 사용자 1 qa.kim 김큐에이(QA), 2 dev.park 박개발(DEV), 3 qa.lee 이큐에이(QA), 4 biz.choi(BIZ), 5 admin / 프로젝트 1 `KB-SAVING` KB 적금 통장 신설, 2 `KB-SWITCH` 갈아타기(과거, RAG 원본) / 요구사항 REQ-001 + 원자 4 + 규칙 4 / TC 11(RULE·RAG·LLM, DRAFT 5) / 1차 통합테스트(PASS4·FAIL1·BLOCKED1·미수행3) / 결함 DF-0001. **인증 도입 전까지 로그인 사용자 = id 1(qa.kim)**.
- 서비스 테스트는 샘플과 분리하려고 테스트 전용 프로젝트(id 99/98)를 `@BeforeEach`에서 넣어 사용.
- 테스트는 `@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:...")`로 인메모리 DB 사용(개발 DB 오염 금지).

## 디자인 v3 (docs/04-DESIGN-v3.md, docs/dashboard-reference.jpg)
- **레이아웃:** 좌측 사이드바 폐기 → 상단 `AppHeader`(로고 AI-TMS + 탭 4개: 대시보드/테스트케이스/테스트 수행/이슈관리, 활성 탭 `--accent` 밑줄) + 헤더 우측 `ProjectSelector` 드롭다운(v4: **전환만**, 등록은 테스트케이스 화면에서만). 페이지 제목(h1) 없음.
- **라이트+다크 모두 지원(2026-09-27~).** 토글은 헤더의 `ThemeToggle`(☀️/🌙), 상태는 `<html data-theme>` + localStorage(`aitms-theme`, 없으면 OS 설정), 적용은 `utils/theme.js`. `main.js`가 마운트 전에 적용해 깜빡임 방지. 색 값은 `theme.css` 하단 `[data-theme='dark']`에 **같은 변수명**으로만 정의(컴포넌트 수정 없음).

### 토큰 (`frontend/src/styles/theme.css`)
1) 문서 정의(그대로 유지): `--surface-page #F5F6FA`, `--surface-card #FFF`, `--border #E7E8EF`, `--text-primary #1F2430`, `--text-secondary #6B7280`, `--text-muted #9CA3AF`, `--accent #4F5FF0`, `--accent-soft #EEF0FE`,
   `--badge-{open|progress|resolved|closed}-{bg|text}`(이슈: 빨강/노랑/초록/회색), `--result-{success|fail|block|notrun}-{bg|text}`(결과: 초록/빨강/**노랑**/**회색**), `--radius-card 12px`, `--shadow-card`
2) 확장(문서에 없는 UI용): `--surface-hover`, `--surface-muted`(pill 트랙), `--accent-hover`, `--on-accent`, `--overlay`, `--shadow-overlay`, `--priority-{high|medium|low}-{bg|text}`(=fail/block/notrun)
3) 레이아웃: `--header-height`, `--content-max-width`, `--space-1..6`(4~32px), `--radius-sm|md|pill`, `--font-size-xs..xl`, `--font-size-stat`(32px), `--font-sans`, `--font-mono` (테마 무관, 다크에서 재정의 안 함)
4) **다크 팔레트**: surface는 네이비(`#17172A`/`#1E1E33`), 배지·결과 색은 원래 색의 낮은 투명도 bg + 밝힌 text(대비 확보, `result-*`는 `badge-*`를 그대로 참조), accent는 `#6C7BFF`(라이트보다 밝게). 우선순위 칩은 `var()` 참조라 재정의 불필요.

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
- 공통 CSS 클래스(base.css): `.card .card-title .btn(.btn-primary/.btn-danger/.btn-sm) .input .select .textarea .label(.required) .table(tr.clickable) .chip(-high/-medium/-low/-muted) .page-actions .empty .muted .mono .error-text .stat-row`(StatCard 4열 그리드, 화면 상단 요약). 새 화면은 이것부터 재사용.
- Frontend API 호출은 `src/api/<도메인>.js` 경유, 에러는 `e.message`를 화면에 표시.
- 프로젝트 종속 화면: `const { currentProjectId: projectId } = storeToRefs(useProjectStore())` + `watch(projectId, load, { immediate: true })`. 선택 변경은 `projectStore.selectProject(id)`로만.
- 라우트: 목록 `/x`, 등록 `/x/new`, 상세 `/x/:id`, 수정 `/x/:id/edit` (등록/수정은 같은 Form 컴포넌트). **예외: 테스트케이스는 등록/수정을 라우트 없이 `TestCaseFormModal` 팝업으로 처리**(목록의 '+ 테스트케이스 추가', 상세의 '수정').
- Backend: 도메인별 패키지, SQL은 mapper XML(`resources/mapper/XxxMapper.xml`)에 작성(어노테이션 SQL 금지), `resultType`은 FQCN 사용(type alias 미사용), DB 컬럼 snake_case → DTO camelCase 자동 매핑.
- 요청 DTO는 record + Bean Validation, 응답/조회 DTO는 Lombok `@Getter @Setter` 클래스.
- 에러: `throw ApiException.notFound(...)/conflict(...)` → `{"message": ...}` 응답. 검증 실패는 400 + 첫 필드 메시지.
- 목록 API는 `PageResponse{items,total,page,size}` 반환, 검색 조건은 `XxxSearch`(page/size/getOffset).
- 로그인 사용자 id는 `CurrentUser.id()`로만 조회 (세션 로그인 principal, '### 인증' 참고). 서비스 코드에서 SecurityContext 를 직접 읽지 말 것.
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

| GET / POST | `/api/executions/{id}/attachments` (multipart `file`, 여러 개) | 실행 결과 증빙 목록 / 업로드(201) |
| GET / DELETE | `/api/executions/{id}/attachments/{attId}[/file?inline=true]` | 다운로드(attachment, 이미지만 inline 허용) / 삭제(업로더·ADMIN) |
| GET / POST / DELETE | `/api/defects/{id}/attachments[...]` | 결함 첨부 — 위와 같은 구조 |
| POST/GET | `/api/auth/login`, `/api/auth/me`, POST `/api/auth/logout` | 로그인(공개) / 현재 사용자 / 로그아웃 |
| GET | `/api/projects`, `/api/projects/{id}/members` | 프로젝트 목록 / 담당자 후보 |
| GET | `/api/cycles?projectId` | 차수 목록 + 결과별 집계(totalCount/passCount/failCount/blockedCount/notRunCount) |
| GET / POST / PUT / DELETE | `/api/cycles[/{id}]` | 차수 CRUD (번호 자동 채번, 생성 시 PLANNED, 이력 있으면 삭제 409) |
| GET | `/api/cycles/{id}/executions?result&assigneeId&keyword` | 차수별 수행 항목 |
| POST | `/api/cycles/{id}/executions` `{testCaseIds, assigneeId}` | TC 등록 (ACTIVE·미등록만, `{added}` 반환) |
| PUT | `/api/cycles/{id}/executions/assignee` `{executionIds, assigneeId}` | 담당자 일괄 지정 (null=해제) |
| DELETE | `/api/cycles/{id}/executions/{executionId}` | 차수에서 제외 (이력 있으면 409) |
| GET | `/api/executions/{id}`, `/api/executions/{id}/history` | 수행 항목(is_draft/draftResult/draftComment 포함) / 이력(최신순) |
| POST | `/api/executions/{id}/draft` `{result, comment}` | **임시저장** — result는 미선택(null) 허용, 확정 결과·이력에는 반영 안 됨 |
| GET | `/api/users` | 가입(비밀번호 있음)·활성 사용자 전체 `{id, displayName}`, 이름순 — 담당자 자유입력 콤보(`UserCombo`)용, 로그인만 하면 조회 가능(권한 체계 없음) |
| POST | `/api/executions/{id}/results` `{result, comment}` | 결과 입력 → 최종결과 갱신 + 이력 추가 |

| GET | `/api/defects?projectId\|executionId&status&unresolved&severity&assigneeId&keyword&page&size` | 결함 검색 (projectId·executionId 중 하나 필수) |
| GET / POST / PUT | `/api/defects[/{id}]` | 결함 상세/등록/수정 (응답에 `nextStatuses`, 삭제 API 없음 → REJECTED) |
| POST | `/api/defects/{id}/status` `{status, comment}` | 상태 전이 (규칙 위반 409) + 이력 기록 |
| GET / POST | `/api/defects/{id}/comments` | 코멘트·상태이력 타임라인 / 코멘트 추가 |
| GET | `/api/dashboard?projectId` | 대시보드 KPI + 내 미수행 TC·내 담당 결함 상위 10건 |

| GET | `/api/test-cases?projectId&excludeProjectId&folderId&unfiled&...` | folderId는 하위 폴더 포함, unfiled=미분류만, projectId 없으면 전체(가져오기 검색) |
| POST | `/api/test-cases/import` `{projectId, testCaseIds, folderId}` (JSON) | 다른 프로젝트 APPROVED·ACTIVE TC를 새 row로 복제 |
| POST | `/api/test-cases/import?projectId=` (multipart `file`, .xlsx) | **엑셀 대량 업로드** → `{successCount, failureCount, failures:[{row, reason}]}` (같은 경로, Content-Type으로 구분) |
| GET | `/api/test-cases/import/template` | 빈 양식(.xlsx, 헤더만 + '작성 안내' 시트) 다운로드 |
| GET / POST | `/api/projects/{id}/folders` `{name, parentFolderId}` | 폴더 트리(roots/totalCount/unfiledCount) / 생성(형제 중 마지막, 같은 이름 409) |
| PUT | `/api/projects/{id}/folders/{folderId}` `{name}` | 폴더 이름 변경 (같은 위치 같은 이름 409, 자기 이름 그대로는 허용) |
| POST | `/api/projects` `{code, name, description, startDate, endDate}` | 프로젝트 등록 (코드 대문자, 등록자 PM 자동 참여) — 화면은 테스트케이스 탭에서만 |
| GET / POST / PUT / DELETE | `/api/test-cases/{id}/datasets[/{rowId}]` `{rowLabel, paramValues:{}, expectedResultOverride}` | 데이터셋 행 CRUD (행 단위 — 실행 항목이 참조, 실행된 행 삭제 409) |
| PUT | `/api/test-cases/{id}/requirements` `{atomicRequirementIds}` | 연결 요구사항 교체 ('연결된 요구사항' 탭) |
| GET | `/api/test-cases/{id}/runs` | 실행 이력 (모든 차수, 시간 역순, 데이터 행 포함) |
| PATCH | `/api/test-cases/{id}/review` `{reviewStatus}` | AI 추천 TC 승인/반려 (검토자=CurrentUser) |
| GET / POST | `/api/requirements?projectId`, `/api/requirements` | 요구사항 목록(원자·연결TC 수) / 등록(MANUAL, REQ-### 채번) |
| GET | `/api/requirements/{id}` | 상세 + 원자 요구사항 + 원자별 커버 TC(`atomics[].testCases`, Traceability) |
| GET | `/api/requirements/atomics?projectId` | 프로젝트 원자 요구사항 전체(원문 코드·제목 포함) — TC 폼 선택용 |
| POST | `/api/requirements/{id}/recommend` | AI 추천 요청 → `recommendation_job`(RUNNING) 생성 후 **202로 잡 즉시 반환**, 추천(RULE+RAG)은 백그라운드 실행. 이미 진행 중인 잡이 있으면 그 잡 반환 |
| GET | `/api/recommendation-jobs/{jobId}` | 잡 상태(PENDING/RUNNING/SUCCEEDED/FAILED, startedAt/finishedAt/errorMessage). SUCCEEDED면 `result{created, skipped, warnings, scores}`(=RecommendResponse) 포함 |
| GET | `/api/recommendation-jobs/latest?requirementId` | 요구사항의 최근 잡 (없으면 204) — 화면 재진입 시 진행/실패 상태 복원 |
| GET | `/api/rule-catalog` | 규칙 카탈로그 |

### 프로젝트 소유 · 중앙관리 규칙 (v4)
- TC는 project_id로 각 프로젝트가 **소유**(Zephyr 구조). 같은 row를 여러 프로젝트가 공유하는 N:N 공유는 **하지 않음**.
- '중앙관리'는 검색/추천 레이어에서: ① AI 추천(RAG)은 project_id 제한 없이 **전체 프로젝트의 APPROVED TC**를 검색 대상으로 ② '다른 프로젝트에서 가져오기' = 새 row로 복제 + `origin_project_id`=원본 프로젝트(요구사항 연결은 복사 안 함, 복제본은 MANUAL·APPROVED).
- 폴더는 같은 프로젝트 안에서만(교차 프로젝트 폴더 지정 400). TC 수정 시 프로젝트 이동 불가, 폴더 이동은 가능.
- 차수에는 **같은 프로젝트의** ACTIVE·APPROVED TC만 등록.
- 폴더 선택 상태는 URL `?folder=all|unfiled|<id>`로 유지(상세→목록 복귀 시 같은 폴더).
- **프로젝트 생성은 헤더 `ProjectSelector` 드롭다운**(맨 아래 '+ 새 프로젝트 만들기' → `NewProjectModal` 팝업, `components/`)에서만. RepoTabs에는 더 이상 없음. 프로젝트 목록·조회 API는 로그인 사용자 누구에게나 공개(멤버십/권한 제한 없음 — RBAC 미도입).
- **프로젝트 필터링 원칙(v4-4):** 목록·집계 API는 모두 `projectId` 쿼리(또는 차수/수행항목/이슈 id처럼 이미 프로젝트가 정해진 경로)로 조회. 예외는 설계상 전체 대상인 것만 — 규칙 카탈로그, '다른 프로젝트에서 가져오기' 검색(`excludeProjectId`), **RAG 검색(3-2: project_id로 제한하지 않고 전체 프로젝트의 APPROVED·ACTIVE TC 대상, 현재 프로젝트 TC 포함 여부는 구현 시 결정)**.
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

- **TC 등록/수정 팝업에는 요구사항 연결이 없음.** 생성 후 상세의 '연결된 요구사항' 탭에서 연결. `PUT /api/test-cases/{id}`는 `atomicRequirementIds`를 **생략(null)하면 기존 연결 유지**, 보내면 전체 교체.
- '다른 프로젝트에서 가져오기' 검색은 `keywordInProjectName=true`로 키워드를 **프로젝트명에도 LIKE** 적용(제목·코드·태그·프로젝트명). 일반 목록 검색은 프로젝트명 조건을 쓰지 않음.

### 엑셀 업로드 규칙 (TestCaseExcelService)
- 첫 시트·첫 행=헤더, 컬럼은 **헤더 이름**으로 찾음(순서 무관): `제목 | 스텝 | 기대결과 | 폴더경로 | 기법`. 제목·스텝·기대결과 필수. 최대 1000행, .xlsx만(5MB, `spring.servlet.multipart`), Apache POI(`poi-ooxml`).
- 행 단위 부분 성공: 누락/형식 오류 행은 `failures`(엑셀 행 번호, 헤더=1행)에 사유와 함께 담고 나머지는 저장(실패 행의 폴더는 만들지 않음). 완전히 빈 행은 무시.
- 스텝·기대결과 셀은 줄바꿈으로 여러 단계('1. ' 번호 제거). 줄 수가 같으면 단계별 짝, 다르면 기대결과 전체를 마지막 단계에. 폴더경로 `A > B > C`는 없는 폴더 자동 생성(`TestCaseFolderService.findOrCreatePath`), 비면 미분류. 기법은 한글 표시명 또는 영문 코드.
- 저장 값: source=MANUAL, **review_status=DRAFT**(승인해야 차수 등록 가능), status=ACTIVE, 우선순위 MEDIUM, 작성자=CurrentUser. ('status=draft' 요청은 이 프로젝트의 review_status DRAFT로 해석)

### AI 추천/검토 규칙
- **추천 잡(recommendation_job):** `RecommendationJobService.start` 가 RUNNING 잡을 만들어 즉시 반환하고 `recommendationExecutor`(스레드풀 2~4)에서 `RequirementService.recommend`를 실행 → 끝나면 SUCCEEDED(결과 요약 `result_json`)/FAILED(`error_message`)로 갱신. 잡 갱신은 추천 트랜잭션과 분리. 서버 시작 시 끝나지 못한 잡은 FAILED 처리. 상태 enum은 프로젝트 규칙대로 대문자. `atomic_requirement_id`는 NULL=요구사항 전체(현재 항상 NULL, 원자 단위 요청용 예약) — 실행 단위 컬럼은 `requirement_id`.
- **화면(요구사항 탭):** '✨ AI 추천 요청' → 잡 생성 → 2초 폴링. `RecommendationStatusBadge`(진행 중 스피너 / 실패 시 클릭 재요청)를 요구사항 행과 원자 요구사항 행에 표시, 성공하면 뱃지 제거 + 결과(생성 DRAFT TC·유사도) 표시 + 목록/커버 TC 자동 새로고침. 재진입 시 latest 잡으로 복원.
- 흐름: 요구사항 원문 → 원자 요구사항 분해 → 타입별 규칙(RULE)/과거 프로젝트 유사 TC(RAG)/신규 생성(LLM) → `DRAFT` TC → 사람이 승인/반려.
- 화면에서 직접 만든 TC = source MANUAL + APPROVED (검토 대상 아님). 출처·검토상태는 TC 수정 API로 못 바꿈.
- **차수 등록은 ACTIVE + APPROVED TC만** (DRAFT/REJECTED는 제외).
- 규칙기반(RULE) 추천 = **파라미터화 TC 1개 + 데이터셋 N행**. `[[text]] [[unit]] [[flag]] [[bonus]]`는 생성 시점에 원자 요구사항 값으로 치환, `{value} {option} {flag} {expected}`는 단계에 남는 데이터셋 변수. 범위 값 누락·규칙 없음은 `warnings`로 반환.
- 저장은 `RequirementService.recommend`가 담당(`TestCaseService.createDraft`), 엔진은 후보(`TcRecommendation`)만 생성. 엔진 추가 = `RecommendationEngine` 빈 추가(LLM 3-3 예정).
- **LLM(3-3) 규칙:** 요구사항 1건당 Claude API 1회(`LlmRecommendationService`) — 원문·원자 요구사항(id/유형/범위/조건)·**이미 연결된 TC 제목**(중복 방지)을 주고, 규칙/RAG가 놓치기 쉬운 교차 조합·예외 흐름·정합성 TC를 최대 `app.ai.llm.max-cases`(5)건 제안받음. 응답은 **구조화 출력**(`LlmProposal` 레코드 → JSON 스키마 자동 생성)이라 파싱 실패가 없음. 검증: 원자 id가 목록에 없거나 제목/단계 누락·길이 초과인 제안은 버리고 경고. 저장은 source=LLM·DRAFT·비파라미터화·미분류·해당 원자 요구사항에 링크(승인 후 사용). 모델 `app.ai.llm.model`(기본 `claude-opus-5`), 인증은 `app.ai.llm.api-key` 또는 SDK 기본(`ANTHROPIC_API_KEY` 환경변수 / `ant auth login`). **호출 실패·인증 없음·거절·잘림은 예외가 아니라 경고**로 남기고 규칙/RAG 결과는 유지. **기본값 `app.ai.llm.enabled: false`(비용 때문에 꺼둠, 2026-09-27 결정)** — 꺼져 있으면 API를 호출하지 않고 결과 경고에 'LLM 추천은 비활성화 상태입니다 (설정 app.ai.llm.enabled=false …)'를 표시하며 규칙기반·RAG는 API 키 없이 정상 동작. 켜려면 `enabled: true` + 키 설정(테스트는 gradle `systemProperty`로 꺼두고 가짜 `LlmClient` 사용). 프롬프트 캐싱·거절 fallback 미적용.
- **RAG(3-2) 규칙:** 검색 대상 = **현재 프로젝트를 제외한** 전체 프로젝트의 APPROVED·ACTIVE TC. 점수 = max(원자 텍스트↔TC 제목·모듈·태그, ↔TC가 검증하는 원자 요구사항 텍스트)의 글자 bigram Dice + 같은 요구사항 유형 +0.1, 임계 0.3 이상, 원자 요구사항당 상위 3건. 같은 원본 TC가 여러 원자에 걸리면 가장 유사한 원자 하나에만. 채택 시 원본의 단계·데이터셋을 복사한 DRAFT(source=RAG, origin_project_id=원본 프로젝트)로 저장하고 링크는 현재 원자 요구사항에 건다. 임베딩 도입 시 `TcRetriever` 구현체만 교체.

### 테스트수행 규칙
- 결과 입력 시 수행자=CurrentUser, 차수가 PLANNED면 IN_PROGRESS로 자동 전환.
- **임시저장**: `ExecutionPanel`의 결과 칩은 클릭해도 즉시 저장되지 않고 선택만 됨 — 패널 상단 '임시저장'(결과 미선택도 가능, 코멘트만도 됨)/'저장'(결과 필수, 기존 `record` 그대로) 버튼으로 확정. 임시저장은 `is_draft`만 갱신하고 이력에 안 남으며, 패널 재진입(`onMounted`) 시 `draftResult`/`draftComment`로 복원. 확정 저장하면 draft 필드는 서버가 비움. 차수 상세 테이블의 `ResultSelect`(행 인라인 즉시 변경)는 이 흐름과 별개 — 그대로 즉시 확정.
- **담당자 선택**: 프로젝트 멤버로 한정하지 않고 `GET /api/users`(가입 사용자 전체)를 `UserCombo`(선택+자유입력, 이름 정확히 일치해야 매핑됨)로 노출. TC 추가 팝업(`TcPickerModal`)·차수 상세 일괄 담당자 지정에 적용. (결함 담당자는 아직 프로젝트 멤버 select — 변경 안 함)
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

### 첨부파일 규칙 (com.aitms.attachment)
- 저장: `app.upload.dir`(기본 `./uploads` = backend 실행 위치, **git 제외**) 아래 `executions/{id}/`·`defects/{id}/`에 **UUID 이름**으로 저장, DB에는 상대 경로·원본 파일명·MIME·크기·업로더. 서버 경로는 API 응답에 노출하지 않음(`@JsonIgnore`).
- 제한: 확장자 화이트리스트(png/jpg/jpeg/gif/webp/bmp/pdf/txt/log/csv/json/zip/xlsx/docx — **svg·html 등 제외**), Content-Type 은 확장자로 결정(클라이언트 값 불신), 파일당 10MB(`spring.servlet.multipart`), 항목당 최대 20개, 빈 파일/허용 안 된 형식 400, 파일명의 경로 구분자·제어문자 제거, 읽을 때도 업로드 루트 이탈 검사.
- 규칙: 대상(실행 항목/결함)이 없으면 404, **CLOSED 차수의 실행 항목은 추가·삭제 409(조회·다운로드는 가능)**, 삭제는 **업로더 또는 ADMIN**만(403), DB 행 삭제 시 디스크 파일도 삭제. 다운로드는 `Content-Disposition: attachment`, `?inline=true`는 이미지에만(썸네일).
- 요청서의 `test_round_case*`/`/api/test-round-cases`는 이 프로젝트 명칭인 `test_execution*`/`/api/executions`로 구현. 두 테이블은 구조가 같아 `AttachmentMapper` 하나가 `AttachmentTarget` enum 상수(`${}`)로 공용 처리(사용자 입력은 절대 `${}`에 넣지 않음).
- 화면: 공용 `AttachmentPanel`(파일 선택·드래그앤드롭·**Ctrl+V 스크린샷 붙여넣기**, 이미지 썸네일/파일 아이콘 목록, 클릭 시 다운로드). 실행 결과 입력 패널에 표시하고 **결과가 FAIL/BLOCKED이며 첨부가 없으면 붉은 안내·펄스 강조**로 첨부 유도. 결함 상세에 표시, 결함 등록/수정 화면에도 있음(등록 시엔 파일을 모아 두었다가 저장 후 업로드, 업로드만 실패하면 이슈는 유지하고 안내).

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
- ✅ 파라미터화 TC (docs/09): A 스키마·백엔드 / B TC 목록(Key·데이터 행 수)·상세 탭(개요·테스트 스크립트·데이터셋·실행 이력·연결된 요구사항)·DatasetTable / C 차수 상세 테이블(행별 집계·치환 표시·ResultSelect)
- ✅ 3-1 규칙기반 추천 (rule_catalog.generator, DRAFT 저장, 테스트 51개)
- ✅ 담당자명 입력(표시용) → Spring Security 로그인으로 대체됨
- ✅ **증빙 첨부**(테스트 수행 결과·결함): 로컬 디스크 저장, AttachmentPanel, FAIL 시 첨부 유도 (테스트 91개)
- ✅ **Spring Security 로그인**: 세션+CSRF, BCrypt, 로그인 화면·가드, `/api/**` 보호, H2 콘솔 ADMIN 전용 (테스트 83개)
- ✅ 엑셀 대량 업로드 (POI, 템플릿 다운로드, 업로드 팝업·결과 표시, 테스트 69개)
- ✅ AI 추천 잡 상태 표시 (recommendation_job, 백그라운드 실행, 상태 뱃지·폴링)
- ✅ 3-2 RAG 추천 (키워드 유사도, 다른 프로젝트만, 테스트 55개, 요구사항 탭에 RAG 출처·유사도 표시)
- ✅ 3-3 LLM 신규 생성 (Claude API, 구조화 출력, 테스트 78개 — 실제 API 호출은 키가 없어 미검증)
- ✅ **다크모드**: `ThemeToggle`, `utils/theme.js`, theme.css 다크 팔레트
- ✅ **StatCard 상단 요약**: 테스트케이스(전체·활성·검토대기·미분류, 검색 API 재사용) / 테스트 수행(전체 차수·진행중·종료·전체 통과율, 이미 불러온 차수 목록에서 클라이언트 계산 — API 추가 없음) / 이슈(전체·미해결·Critical·종료)
- ⏳ **다음:** ai-agent(FastAPI /decompose)·임베딩 RAG·LLM 연동은 예산 정해지면. 그 외 후보: TC 단계 스냅샷, 프로젝트/사용자 관리 화면, 역할별 권한(RBAC) ai-agent(FastAPI /decompose)·임베딩 RAG·LLM 연동은 예산이 정해진 뒤로 보류
- ⏳ 이후 후보: ai-agent(FastAPI /decompose) + AiAgentClient 뼈대, 다크모드 값, 테스트케이스/수행/이슈 화면 상단 StatCard, Spring Security 로그인(CurrentUser 교체), TC 단계 스냅샷, 프로젝트/사용자 관리 화면
