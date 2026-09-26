-- 테스트케이스 ↔ 요구사항 다대다(N:N) 매핑 (Zephyr Traceability 대응)
-- 기존 test_case.atomic_requirement_id 단일 FK 대신 조인 테이블로 전환

CREATE TABLE IF NOT EXISTS test_case_requirement_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_case_id BIGINT NOT NULL,
    atomic_requirement_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (test_case_id) REFERENCES test_case(id),
    FOREIGN KEY (atomic_requirement_id) REFERENCES atomic_requirement(id),
    UNIQUE (test_case_id, atomic_requirement_id)
);

-- 기존 데이터 마이그레이션: test_case.atomic_requirement_id에 있던 단일 연결을 조인테이블로 이관
INSERT INTO test_case_requirement_link (test_case_id, atomic_requirement_id)
SELECT id, atomic_requirement_id FROM test_case WHERE atomic_requirement_id IS NOT NULL;

-- 이후 test_case.atomic_requirement_id 컬럼은 더 이상 사용하지 않음 (호환을 위해 컬럼 자체는 남겨두거나,
-- 정리하고 싶으면 아래 주석 해제)
-- ALTER TABLE test_case DROP COLUMN atomic_requirement_id;

-- 예시: 테스트케이스 하나가 여러 요구사항을 커버하는 경우 추가
-- (10번 케이스 "최대 가입금액+최장 거치기간 동시 검증"은 원래 amount_range 요구사항 하나에만 연결돼 있었는데,
--  실제로는 period_condition 요구사항도 같이 검증하므로 링크 추가)
INSERT INTO test_case_requirement_link (test_case_id, atomic_requirement_id) VALUES (10, 3);
