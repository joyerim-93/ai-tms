-- Parameterized Test Case (데이터 기반 반복 실행) 추가
-- Zephyr의 "Test Case with Data-Driven Datasets" 대응

-- 1. test_case에 파라미터화 여부 플래그 추가
ALTER TABLE test_case ADD COLUMN IF NOT EXISTS is_parameterized BOOLEAN DEFAULT FALSE;

-- 2. 데이터셋 테이블: 파라미터화된 TC 하나에 여러 행(row)의 입력값 세트
CREATE TABLE IF NOT EXISTS test_case_dataset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_case_id BIGINT NOT NULL,
    row_label VARCHAR(200) NOT NULL,        -- 예: "최소금액-1", "최소금액", "최대금액", "최대금액+1"
    param_values VARCHAR(2000) NOT NULL,    -- JSON: {"amount": 9999}
    expected_result_override VARCHAR(2000), -- 이 행만 다른 기대결과가 필요하면 (없으면 TC의 expected_result 사용)
    sort_order INT DEFAULT 0,
    FOREIGN KEY (test_case_id) REFERENCES test_case(id)
);

-- 3. 차수별 실행결과에 dataset_id 연결 (파라미터화 TC는 데이터셋 행마다 결과가 따로 남음)
ALTER TABLE test_round_case ADD COLUMN IF NOT EXISTS dataset_id BIGINT;
ALTER TABLE test_round_case ADD CONSTRAINT IF NOT EXISTS fk_trc_dataset
    FOREIGN KEY (dataset_id) REFERENCES test_case_dataset(id);
-- dataset_id가 NULL이면 기존처럼 TC 전체 결과, 값이 있으면 그 데이터셋 행에 대한 개별 결과

-- ============================================
-- 예시: 기존 가입금액 경계값 TC 4개(id 1~4)를
-- 파라미터화 TC 1개 + 데이터셋 4행으로 재구성
-- ============================================

-- 새 파라미터화 TC (steps에 {amount} 플레이스홀더 사용)
INSERT INTO test_case (id, atomic_requirement_id, project_id, folder_id, title, steps, expected_result, technique, source, status, is_parameterized, created_by) VALUES
  (12, 1, 1, 3, '가입금액 경계값 검증 (데이터 기반)',
   '1. 가입금액에 {amount}원 입력\n2. 가입 신청',
   '{expected}',
   'boundary_value', 'rule', 'approved', TRUE, 'system');

-- 데이터셋 4행
INSERT INTO test_case_dataset (id, test_case_id, row_label, param_values, expected_result_override, sort_order) VALUES
  (1, 12, '최소금액 미만(9,999원)', '{"amount": 9999}', '"최소 가입금액은 10,000원입니다" 오류, 가입 실패', 1),
  (2, 12, '최소금액(10,000원)',     '{"amount": 10000}', '가입 정상 처리', 2),
  (3, 12, '최대금액(3,000,000원)',  '{"amount": 3000000}', '가입 정상 처리', 3),
  (4, 12, '최대금액 초과(3,000,001원)', '{"amount": 3000001}', '"최대 가입금액은 3,000,000원입니다" 오류, 가입 실패', 4);

-- 참고: 기존 개별 TC 1~4는 그대로 두거나(과거 이력 보존), 정리하고 싶으면 status를 'rejected'로 바꿔서
-- "파라미터화 TC(12)로 대체됨"을 표시하는 방식을 권장합니다. (삭제는 실행 이력 때문에 지양)
UPDATE test_case SET status = 'rejected' WHERE id IN (1, 2, 3, 4);

-- 테스트 차수(1차)에 파라미터화 TC를 등록하면, 데이터셋 행마다 개별 실행결과가 생성됨
INSERT INTO test_round_case (id, test_round_id, test_case_id, dataset_id, result, executed_by, executed_at, comment) VALUES
  (10, 1, 12, 1, 'success', 'qa.kim', '2026-09-24 10:00:00', NULL),
  (11, 1, 12, 2, 'success', 'qa.kim', '2026-09-24 10:05:00', NULL),
  (12, 1, 12, 3, 'fail',    'qa.kim', '2026-09-24 10:10:00', '기존 결함(ISSUE-1)과 동일 증상 재현'),
  (13, 1, 12, 4, 'success', 'qa.kim', '2026-09-24 10:15:00', NULL);
