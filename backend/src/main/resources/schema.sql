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
    template         VARCHAR(2000) NOT NULL            -- {min}, {max} 치환
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
    tc_code               VARCHAR(30)  NOT NULL UNIQUE,        -- 예: TC-00001
    project_id            BIGINT       NOT NULL REFERENCES project (id),
    folder_id             BIGINT       REFERENCES test_case_folder (id) ON DELETE SET NULL,  -- NULL = 미분류
    title                 VARCHAR(300) NOT NULL,
    module                VARCHAR(100),                        -- 업무 분류
    precondition          VARCHAR(2000),
    priority              VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DEPRECATED')),
    tags                  VARCHAR(500),                        -- 콤마 구분
    -- 추천/검토 (AI 추천 결과는 DRAFT로 들어와 검토 후 APPROVED)
    source                VARCHAR(10)  NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'RULE', 'RAG', 'LLM')),
    technique             VARCHAR(30)
                          CHECK (technique IN ('BOUNDARY_VALUE', 'EQUIVALENCE_PARTITION', 'DECISION_TABLE', 'EXPLORATORY')),
    review_status         VARCHAR(10)  NOT NULL DEFAULT 'APPROVED' CHECK (review_status IN ('DRAFT', 'APPROVED', 'REJECTED')),
    atomic_requirement_id BIGINT       REFERENCES atomic_requirement (id) ON DELETE SET NULL,
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
CREATE INDEX IF NOT EXISTS idx_test_case_atomic ON test_case (atomic_requirement_id);

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
    tc_version   INT          NOT NULL,               -- 등록 시점 TC 버전 스냅샷
    assignee_id  BIGINT       REFERENCES users (id),
    result       VARCHAR(10)  NOT NULL DEFAULT 'NOT_RUN' CHECK (result IN ('PASS', 'FAIL', 'BLOCKED', 'NOT_RUN')),
    executed_by  BIGINT       REFERENCES users (id),  -- 마지막 수행자
    executed_at  TIMESTAMP,                           -- 마지막 수행 시각
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (cycle_id, test_case_id)
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

