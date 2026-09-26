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

-- ─────────────────────────────── ② 테스트케이스 저장소
CREATE TABLE IF NOT EXISTS test_case (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tc_code      VARCHAR(30)  NOT NULL UNIQUE,        -- 예: TC-00001
    title        VARCHAR(200) NOT NULL,
    module       VARCHAR(100),                        -- 업무 분류
    precondition VARCHAR(2000),
    priority     VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DEPRECATED')),
    tags         VARCHAR(500),                        -- 콤마 구분
    author_id    BIGINT       REFERENCES users (id),
    version      INT          NOT NULL DEFAULT 1,     -- 내용 수정 시 +1
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_test_case_module ON test_case (module);

CREATE TABLE IF NOT EXISTS test_step (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_case_id    BIGINT        NOT NULL REFERENCES test_case (id) ON DELETE CASCADE,
    step_no         INT           NOT NULL,
    action          VARCHAR(2000) NOT NULL,
    expected_result VARCHAR(2000),
    UNIQUE (test_case_id, step_no)
);

CREATE TABLE IF NOT EXISTS requirement (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES project (id),
    req_code    VARCHAR(30)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description CLOB,                                 -- RAG 입력 원문
    priority    VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, req_code)
);

CREATE TABLE IF NOT EXISTS requirement_tc (
    requirement_id BIGINT       NOT NULL REFERENCES requirement (id) ON DELETE CASCADE,
    test_case_id   BIGINT       NOT NULL REFERENCES test_case (id),
    source         VARCHAR(10)  NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'AI')),
    score          DECIMAL(5, 4),                      -- AI 추천 유사도 (MANUAL은 NULL)
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (requirement_id, test_case_id)
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

-- ─────────────────────────────── 코드 채번 시퀀스
CREATE SEQUENCE IF NOT EXISTS tc_code_seq START WITH 1;
