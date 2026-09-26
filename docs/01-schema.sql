-- ai-tms H2 schema (초안)
-- backend/src/main/resources/schema.sql 로 사용

CREATE TABLE IF NOT EXISTS project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS requirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    raw_text VARCHAR(4000) NOT NULL,
    source VARCHAR(20) DEFAULT 'manual', -- manual | pms
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE IF NOT EXISTS atomic_requirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requirement_id BIGINT NOT NULL,
    atomic_text VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL, -- amount_range | rate_range | period_condition | boolean_flag
    min_value DECIMAL(18,2),
    max_value DECIMAL(18,2),
    unit VARCHAR(20),
    conditions VARCHAR(2000), -- JSON string
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (requirement_id) REFERENCES requirement(id)
);

CREATE TABLE IF NOT EXISTS rule_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requirement_type VARCHAR(50) NOT NULL,
    technique VARCHAR(50) NOT NULL, -- boundary_value | equivalence_partition | decision_table
    template VARCHAR(2000) NOT NULL
);

CREATE TABLE IF NOT EXISTS test_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    atomic_requirement_id BIGINT,
    project_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    steps VARCHAR(4000),
    expected_result VARCHAR(2000),
    technique VARCHAR(50), -- boundary_value | equivalence_partition | decision_table | exploratory
    source VARCHAR(20) NOT NULL, -- rule | rag | llm | manual
    status VARCHAR(20) DEFAULT 'draft', -- draft | approved | rejected
    origin_project_id BIGINT, -- RAG로 가져온 경우 원본 프로젝트
    created_by VARCHAR(100),
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (atomic_requirement_id) REFERENCES atomic_requirement(id),
    FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE IF NOT EXISTS test_round (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL, -- 예: "1차 통합테스트"
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'planned', -- planned | in_progress | closed
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE IF NOT EXISTS test_round_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_round_id BIGINT NOT NULL,
    test_case_id BIGINT NOT NULL,
    result VARCHAR(20) DEFAULT 'not_executed', -- success | fail | block | not_executed
    executed_by VARCHAR(100),
    executed_at TIMESTAMP,
    comment VARCHAR(1000),
    FOREIGN KEY (test_round_id) REFERENCES test_round(id),
    FOREIGN KEY (test_case_id) REFERENCES test_case(id)
);

CREATE TABLE IF NOT EXISTS defect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_round_case_id BIGINT,
    project_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    description VARCHAR(4000),
    severity VARCHAR(20) DEFAULT 'medium', -- critical | high | medium | low
    status VARCHAR(20) DEFAULT 'open', -- open | in_progress | resolved | closed
    assignee VARCHAR(100),
    reporter VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (test_round_case_id) REFERENCES test_round_case(id),
    FOREIGN KEY (project_id) REFERENCES project(id)
);
