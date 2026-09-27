-- AI-TMS 스키마 (H2, MySQL 모드)
-- 기동 시마다 실행됨 → IF NOT EXISTS 필수.
-- 기존 테이블 구조 변경 시: 개발 중에는 backend/data/ 삭제 후 재기동.

-- ─────────────────────────────── 공통
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id    VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(50)  NOT NULL,
    email       VARCHAR(100),
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('DEV', 'BIZ', 'QA', 'ADMIN')),
    password    VARCHAR(200),                        -- Spring Security 도입 시 사용
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS project (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CLOSED')),
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS project_member (
    project_id   BIGINT      NOT NULL REFERENCES project (id),
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    project_role VARCHAR(20) NOT NULL CHECK (project_role IN ('PM', 'DEV', 'BIZ', 'QA')),
    PRIMARY KEY (project_id, user_id)
);

-- ─────────────────────────────── ② 요구사항 (AI 추천 입력)
CREATE TABLE IF NOT EXISTS requirement (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES project (id),
    req_code    VARCHAR(30)  NOT NULL,                -- 예: REQ-001 (프로젝트 내 유일)
    title       VARCHAR(200) NOT NULL,
    description CLOB,                                 -- 요구사항 원문(raw text)
    source      VARCHAR(10)  NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'PMS')),
    priority    VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, req_code)
);

-- AI 에이전트가 원문을 분해한 원자 요구사항
CREATE TABLE IF NOT EXISTS atomic_requirement (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    requirement_id BIGINT         NOT NULL REFERENCES requirement (id) ON DELETE CASCADE,
    atomic_text    VARCHAR(1000)  NOT NULL,
    type           VARCHAR(30)    NOT NULL
                   CHECK (type IN ('AMOUNT_RANGE', 'RATE_RANGE', 'PERIOD_CONDITION', 'BOOLEAN_FLAG')),
    min_value      DECIMAL(18, 2),
    max_value      DECIMAL(18, 2),
    unit           VARCHAR(20),                        -- KRW, percent, year ...
    conditions     VARCHAR(2000),                      -- JSON 문자열
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 규칙기반 추천용: 요구사항 타입별 정형 테스트 기법 템플릿
CREATE TABLE IF NOT EXISTS rule_catalog (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    requirement_type VARCHAR(30)   NOT NULL
                     CHECK (requirement_type IN ('AMOUNT_RANGE', 'RATE_RANGE', 'PERIOD_CONDITION', 'BOOLEAN_FLAG')),
    technique        VARCHAR(30)   NOT NULL
                     CHECK (technique IN ('BOUNDARY_VALUE', 'EQUIVALENCE_PARTITION', 'DECISION_TABLE')),
    template         VARCHAR(2000) NOT NULL,           -- 사람이 읽는 규칙 설명 ({min}, {max})
    -- 추천 생성 규칙 JSON: 파라미터화 TC 1개 + 데이터셋 N행
    --   [[text]] [[unit]] [[flag]] [[bonus]] = 생성 시점 치환(원자 요구사항 값)
    --   {value} {option} {flag} {expected} = TC 단계에 남는 데이터셋 변수
    generator        VARCHAR(4000)
);

-- ─────────────────────────────── ② 테스트케이스 폴더 (프로젝트별 트리, 다단계 중첩)
CREATE TABLE IF NOT EXISTS test_case_folder (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id       BIGINT       NOT NULL REFERENCES project (id),
    parent_folder_id BIGINT       REFERENCES test_case_folder (id),  -- NULL = 프로젝트 최상위
    name             VARCHAR(200) NOT NULL,
    sort_order       INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_folder_project ON test_case_folder (project_id, parent_folder_id);

-- ─────────────────────────────── ② 테스트케이스 (프로젝트 소유. '중앙관리'는 검색/추천/가져오기 레이어에서)
CREATE TABLE IF NOT EXISTS test_case (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tc_code               VARCHAR(30)  NOT NULL UNIQUE,        -- 화면 표기 'Key' (예: TC-101, 전역 순번)
    project_id            BIGINT       NOT NULL REFERENCES project (id),
    folder_id             BIGINT       REFERENCES test_case_folder (id) ON DELETE SET NULL,  -- NULL = 미분류
    title                 VARCHAR(300) NOT NULL,
    module                VARCHAR(100),                        -- 업무 분류
    precondition          VARCHAR(2000),
    priority              VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DEPRECATED')),
    tags                  VARCHAR(500),                        -- 콤마 구분
    is_parameterized      BOOLEAN      NOT NULL DEFAULT FALSE, -- 데이터 기반 반복 실행(데이터셋 행마다 실행)
    -- 추천/검토 (AI 추천 결과는 DRAFT로 들어와 검토 후 APPROVED)
    source                VARCHAR(10)  NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'RULE', 'RAG', 'LLM')),
    technique             VARCHAR(30)
                          CHECK (technique IN ('BOUNDARY_VALUE', 'EQUIVALENCE_PARTITION', 'DECISION_TABLE', 'EXPLORATORY')),
    review_status         VARCHAR(10)  NOT NULL DEFAULT 'APPROVED' CHECK (review_status IN ('DRAFT', 'APPROVED', 'REJECTED')),
    origin_project_id     BIGINT       REFERENCES project (id),  -- RAG 추천·'다른 프로젝트에서 가져오기' 복제 시 원본 프로젝트
    reviewed_by           BIGINT       REFERENCES users (id),
    reviewed_at           TIMESTAMP,
    author_id             BIGINT       REFERENCES users (id),  -- NULL = 시스템/AI 생성
    version               INT          NOT NULL DEFAULT 1,     -- 내용 수정 시 +1
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_test_case_module ON test_case (module);
CREATE INDEX IF NOT EXISTS idx_test_case_project_folder ON test_case (project_id, folder_id);

-- 파라미터화 TC 데이터셋 (docs/08) — 단계 텍스트의 {변수}를 행마다 param_values로 치환해 반복 실행
CREATE TABLE IF NOT EXISTS test_case_dataset (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_case_id             BIGINT        NOT NULL REFERENCES test_case (id) ON DELETE CASCADE,
    row_label                VARCHAR(200)  NOT NULL,          -- 예: 최소금액 미만(9,999원)
    param_values             VARCHAR(2000) NOT NULL,          -- JSON {"amount": 9999}
    expected_result_override VARCHAR(2000),                   -- 이 행 전용 기대결과 → {expected} 치환값
    sort_order               INT           NOT NULL DEFAULT 0,
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_dataset_tc ON test_case_dataset (test_case_id, sort_order);

-- TC ↔ 원자 요구사항 다대다 (Zephyr Traceability, docs/07-schema-add-requirement-link.sql)
-- 같은 프로젝트의 요구사항만 연결 (서비스에서 검증)
CREATE TABLE IF NOT EXISTS test_case_requirement_link (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_case_id          BIGINT    NOT NULL REFERENCES test_case (id) ON DELETE CASCADE,
    atomic_requirement_id BIGINT    NOT NULL REFERENCES atomic_requirement (id) ON DELETE CASCADE,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (test_case_id, atomic_requirement_id)
);
CREATE INDEX IF NOT EXISTS idx_tc_req_link_atomic ON test_case_requirement_link (atomic_requirement_id);

CREATE TABLE IF NOT EXISTS test_step (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_case_id    BIGINT        NOT NULL REFERENCES test_case (id) ON DELETE CASCADE,
    step_no         INT           NOT NULL,
    action          VARCHAR(2000) NOT NULL,
    expected_result VARCHAR(2000),
    UNIQUE (test_case_id, step_no)
);

-- ─────────────────────────────── ③ 테스트수행관리
CREATE TABLE IF NOT EXISTS test_cycle (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES project (id),
    cycle_no    INT          NOT NULL,                -- 차수
    name        VARCHAR(100) NOT NULL,
    start_date  DATE,
    end_date    DATE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'CLOSED')),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, cycle_no)
);

-- 차수 × TC 의 현재(최종) 상태
CREATE TABLE IF NOT EXISTS test_execution (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    cycle_id     BIGINT       NOT NULL REFERENCES test_cycle (id) ON DELETE CASCADE,
    test_case_id BIGINT       NOT NULL REFERENCES test_case (id),
    dataset_id   BIGINT       REFERENCES test_case_dataset (id), -- 파라미터화 TC면 데이터셋 행별 실행, NULL이면 TC 단위
    tc_version   INT          NOT NULL,               -- 등록 시점 TC 버전 스냅샷
    assignee_id  BIGINT       REFERENCES users (id),
    result       VARCHAR(10)  NOT NULL DEFAULT 'NOT_RUN' CHECK (result IN ('PASS', 'FAIL', 'BLOCKED', 'NOT_RUN')),
    executed_by  BIGINT       REFERENCES users (id),  -- 마지막 수행자
    executed_at  TIMESTAMP,                           -- 마지막 수행 시각
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (cycle_id, test_case_id, dataset_id)       -- dataset_id NULL 중복은 등록 SQL(NOT EXISTS)에서 방지
);
CREATE INDEX IF NOT EXISTS idx_execution_assignee ON test_execution (assignee_id, result);

-- 수행할 때마다 1행 추가 (재수행 이력)
CREATE TABLE IF NOT EXISTS test_execution_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT        NOT NULL REFERENCES test_execution (id) ON DELETE CASCADE,
    result       VARCHAR(10)   NOT NULL CHECK (result IN ('PASS', 'FAIL', 'BLOCKED', 'NOT_RUN')),
    comment      VARCHAR(2000),
    executed_by  BIGINT        REFERENCES users (id),
    executed_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_exec_history_execution ON test_execution_history (execution_id, executed_at);

-- ─────────────────────────────── ④ 결함관리
CREATE TABLE IF NOT EXISTS defect (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id   BIGINT       NOT NULL REFERENCES project (id),
    defect_code  VARCHAR(30)  NOT NULL,               -- 예: DF-0001 (프로젝트 내 유일)
    title        VARCHAR(200) NOT NULL,
    description  CLOB,
    severity     VARCHAR(10)  NOT NULL DEFAULT 'MAJOR' CHECK (severity IN ('CRITICAL', 'MAJOR', 'MINOR', 'TRIVIAL')),
    priority     VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    status       VARCHAR(20)  NOT NULL DEFAULT 'NEW'
                 CHECK (status IN ('NEW', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED')),
    reporter_id  BIGINT       NOT NULL REFERENCES users (id),
    assignee_id  BIGINT       REFERENCES users (id),
    execution_id BIGINT       REFERENCES test_execution (id) ON DELETE SET NULL,  -- 실패 결과에서 등록 시
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, defect_code)
);
CREATE INDEX IF NOT EXISTS idx_defect_assignee ON defect (assignee_id, status);

CREATE TABLE IF NOT EXISTS defect_comment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    defect_id   BIGINT        NOT NULL REFERENCES defect (id) ON DELETE CASCADE,
    author_id   BIGINT        NOT NULL REFERENCES users (id),
    content     VARCHAR(4000),
    status_from VARCHAR(20),                          -- 상태 변경 이력용 (단순 코멘트면 NULL)
    status_to   VARCHAR(20),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- AI 추천 실행 이력/상태 — POST /recommend 가 잡을 만들고 즉시 반환, 완료 시 SUCCEEDED/FAILED 로 갱신 (비동기 대비)
CREATE TABLE IF NOT EXISTS recommendation_job (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    requirement_id        BIGINT        NOT NULL REFERENCES requirement (id) ON DELETE CASCADE,  -- 실행 단위(원문 요구사항 → 원자 전체)
    atomic_requirement_id BIGINT        REFERENCES atomic_requirement (id) ON DELETE CASCADE,    -- NULL = 요구사항 전체 (원자 단위 요청용 예약)
    status                VARCHAR(10)   NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    started_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at           TIMESTAMP,
    error_message         VARCHAR(1000),
    result_json           TEXT                                                                   -- 성공 시 결과 요약 {createdIds, skipped, warnings, scores}
);
CREATE INDEX IF NOT EXISTS idx_rec_job_requirement ON recommendation_job (requirement_id, id);

-- 증빙 첨부파일 — 파일은 app.upload.dir(기본 backend/uploads/) 디스크에 저장, DB에는 상대 경로만 (docs 의 test_round_case = 이 프로젝트의 test_execution)
CREATE TABLE IF NOT EXISTS test_execution_attachment (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT        NOT NULL REFERENCES test_execution (id) ON DELETE CASCADE,
    file_name    VARCHAR(255)  NOT NULL,                  -- 원본 파일명(표시·다운로드용)
    file_path    VARCHAR(500)  NOT NULL,                  -- 업로드 루트 기준 상대 경로 (예: executions/12/3f2a….png)
    content_type VARCHAR(100)  NOT NULL,
    file_size    BIGINT        NOT NULL,
    uploaded_by  BIGINT        NOT NULL REFERENCES users (id),
    uploaded_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_exec_attachment ON test_execution_attachment (execution_id);

-- 결함 첨부파일 (재현 스크린샷 등) — 위와 같은 구조
CREATE TABLE IF NOT EXISTS defect_attachment (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    defect_id    BIGINT        NOT NULL REFERENCES defect (id) ON DELETE CASCADE,
    file_name    VARCHAR(255)  NOT NULL,
    file_path    VARCHAR(500)  NOT NULL,
    content_type VARCHAR(100)  NOT NULL,
    file_size    BIGINT        NOT NULL,
    uploaded_by  BIGINT        NOT NULL REFERENCES users (id),
    uploaded_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_defect_attachment ON defect_attachment (defect_id);
