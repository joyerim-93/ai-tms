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
│     │  ├─ common/              공통(HealthController 등)
│     │  └─ <도메인>/            controller · service · mapper(인터페이스) · dto — 도메인별 패키지
│     └─ resources/
│        ├─ application.yml
│        ├─ schema.sql           (예정) CREATE TABLE IF NOT EXISTS
│        └─ mapper/<도메인>/*.xml MyBatis 쿼리
└─ frontend/                     Vue 3 + Vite
   └─ src/
      ├─ styles/tokens.css       디자인 토큰 (라이트/다크)
      ├─ styles/base.css         리셋 + 공통 클래스(.card, .btn)
      ├─ composables/useTheme.js 테마 토글
      ├─ layouts/AppLayout.vue   사이드바 + 헤더
      ├─ components/             KpiCard, StatusBadge ...
      ├─ views/                  페이지
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
- 상태 표시는 `<StatusBadge status="PASS|FAIL|BLOCKED|NOT_RUN" />` 사용.
- Backend: 도메인별 패키지, SQL은 mapper XML에 작성(어노테이션 SQL 지양), DB 컬럼은 snake_case → DTO는 camelCase(자동 매핑).
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
