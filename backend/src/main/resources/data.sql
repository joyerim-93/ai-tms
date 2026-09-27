-- 초기 샘플 데이터 — 기동마다 실행됨.
-- INSERT IGNORE: 이미 있는 id는 건너뜀 (사용자가 바꾼 데이터를 샘플 값으로 되돌리지 않음)
-- 시나리오: KB 적금 통장 신설 프로젝트 + 유사 과거 프로젝트(자유적금 갈아타기) — docs/02-sample-data.sql 을 현 스키마로 변환
-- 인증 도입 전까지 "로그인 사용자"는 id=1(qa.kim)

-- 1. 사용자
INSERT IGNORE INTO users (id, login_id, name, email, role) VALUES
    (1, 'qa.kim',   '김큐에이', 'qa.kim@aitms.local',   'QA'),
    (2, 'dev.park', '박개발',   'dev.park@aitms.local', 'DEV'),
    (3, 'qa.lee',   '이큐에이', 'qa.lee@aitms.local',   'QA'),
    (4, 'biz.choi', '최현업',   'biz.choi@aitms.local', 'BIZ'),
    (5, 'admin',    '관리자',   'admin@aitms.local',    'ADMIN');

-- 로그인 시드 계정: username=3171613 / password=3171613 (BCrypt 해시로 저장 — 개발용, 운영에서는 변경/삭제)
INSERT IGNORE INTO users (login_id, name, role, password) VALUES
    ('3171613', '3171613', 'QA', '$2a$10$TVu5Zo1PFdF57jZT/82iTuZVAkPzyxrf9IOyydsYpYrdJ.Z.pDDI.');

-- 2. 프로젝트
INSERT IGNORE INTO project (id, code, name, description, status, start_date, end_date) VALUES
    (1, 'KB-SAVING', 'KB 적금 통장 신설',
        '신규 적금 상품 출시 프로젝트. 금리 2.5~3.9%, 거치기간 1/2/3년, 가입금액 1만~300만원',
        'ACTIVE', DATE '2026-09-01', DATE '2026-12-31'),
    (2, 'KB-SWITCH', 'KB 자유적금 갈아타기 이벤트',
        '기존 적금 고객 대상 우대금리 갈아타기 이벤트 (과거 프로젝트, RAG 검색 대상)',
        'CLOSED', DATE '2026-03-01', DATE '2026-05-31');

INSERT IGNORE INTO project_member (project_id, user_id, project_role) VALUES
    (1, 1, 'QA'),
    (1, 2, 'DEV'),
    (1, 3, 'QA'),
    (1, 4, 'BIZ'),
    (1, 5, 'PM');

-- 3. 요구사항 (원문)
INSERT IGNORE INTO requirement (id, project_id, req_code, title, description, source, priority) VALUES
    (1, 1, 'REQ-001', 'KB 적금 상품 가입 조건',
        'KB 적금 통장 신설. 금리는 2.5~3.9%이고, 금리에 영향을 주는 것은 거치기간이 1년/2년/3년이냐에 따라 다름. 가입금액은 최소 1만원, 최대 300만원.',
        'MANUAL', 'HIGH'),
    (2, 2, 'REQ-001', '갈아타기 우대금리',
        '자유적금 갈아타기 시 기존 고객은 우대금리 0.2%p 추가 제공. 갈아타기 신청 기간은 이벤트 시작일로부터 30일 이내.',
        'MANUAL', 'MEDIUM');

-- 4. 원자 요구사항 (AI 에이전트 분해 결과 예시)
INSERT IGNORE INTO atomic_requirement (id, requirement_id, atomic_text, type, min_value, max_value, unit, conditions) VALUES
    (1, 1, '가입금액은 최소 1만원, 최대 300만원이다', 'AMOUNT_RANGE', 10000, 3000000, 'KRW', NULL),
    (2, 1, '금리는 2.5%에서 3.9% 사이이다', 'RATE_RANGE', 2.5, 3.9, 'percent', NULL),
    (3, 1, '거치기간(1년/2년/3년)에 따라 적용 금리가 다르다', 'PERIOD_CONDITION', NULL, NULL, 'year',
        '{"options":["1년","2년","3년"],"rate_map":{"1년":2.5,"2년":3.2,"3년":3.9}}'),
    (4, 2, '기존 고객 갈아타기 시 우대금리 0.2%p가 추가된다', 'BOOLEAN_FLAG', NULL, NULL, NULL,
        '{"flag":"existing_customer","bonus_rate":0.2}');

-- 5. 규칙 카탈로그 — generator: 규칙기반 추천이 만드는 '파라미터화 TC 1개 + 데이터셋 N행' 정의
--    rows[].value: min / max / min-1 / min+1 / max-1 / max+1 (step 단위), rowsFrom=options: 조건 선택지마다 1행
INSERT IGNORE INTO rule_catalog (id, requirement_type, technique, template, generator) VALUES
    (1, 'AMOUNT_RANGE', 'BOUNDARY_VALUE',
        '경계값 테스트: {min}-1(실패), {min}(성공), {min}+1(성공), {max}-1(성공), {max}(성공), {max}+1(실패)',
        '{"title": "[[text]] — 경계값 분석", "step": 1,
          "steps": [{"action": "입력값에 {value}[[unit]] 입력", "expected": null},
                    {"action": "요청 처리", "expected": "{expected}"}],
          "rows": [{"label": "최소값 미만", "value": "min-1", "expected": "허용 범위 밖 — 오류 안내, 처리 거부"},
                   {"label": "최소값", "value": "min", "expected": "정상 처리"},
                   {"label": "최소값 초과", "value": "min+1", "expected": "정상 처리"},
                   {"label": "최대값 미만", "value": "max-1", "expected": "정상 처리"},
                   {"label": "최대값", "value": "max", "expected": "정상 처리"},
                   {"label": "최대값 초과", "value": "max+1", "expected": "허용 범위 밖 — 오류 안내, 처리 거부"}]}'),
    (2, 'RATE_RANGE', 'BOUNDARY_VALUE',
        '경계값 테스트: {min}% 미만(적용불가), {min}%(최소금리 적용), {max}%(최대금리 적용), {max}% 초과(적용불가)',
        '{"title": "[[text]] — 경계값 분석", "step": 0.01,
          "steps": [{"action": "적용 값 {value}[[unit]] 로 설정", "expected": null},
                    {"action": "적용 결과 확인", "expected": "{expected}"}],
          "rows": [{"label": "최소값 미만", "value": "min-1", "expected": "범위 밖 — 적용 불가"},
                   {"label": "최소값", "value": "min", "expected": "최소값 적용"},
                   {"label": "최대값", "value": "max", "expected": "최대값 적용"},
                   {"label": "최대값 초과", "value": "max+1", "expected": "범위 밖 — 적용 불가"}]}'),
    (3, 'PERIOD_CONDITION', 'DECISION_TABLE',
        '조건별 디시전테이블: 거치기간 옵션 각각에 대해 매핑된 금리가 정확히 적용되는지 검증',
        '{"title": "[[text]] — 조건별 결정 테이블",
          "steps": [{"action": "조건 {option} 선택", "expected": null},
                    {"action": "처리 진행 후 적용 값 확인", "expected": "{expected}"}],
          "rowsFrom": "options", "label": "{option}", "expected": "{option} 선택 시 매핑 값 {mapped} 적용"}'),
    (4, 'BOOLEAN_FLAG', 'EQUIVALENCE_PARTITION',
        '동등분할: 플래그 true(우대금리 적용됨), 플래그 false(우대금리 미적용)',
        '{"title": "[[text]] — 동등 분할",
          "steps": [{"action": "[[flag]] = {flag} 인 대상으로 진행", "expected": null},
                    {"action": "결과 확인", "expected": "{expected}"}],
          "rows": [{"label": "조건 충족", "flag": true, "expected": "혜택 적용[[bonus]]"},
                   {"label": "조건 미충족", "flag": false, "expected": "혜택 미적용"}]}');

-- 6-0. 테스트케이스 폴더 (docs/05-schema-add-folders.sql)
INSERT IGNORE INTO test_case_folder (id, project_id, parent_folder_id, name, sort_order) VALUES
    (1, 1, NULL, '가입 프로세스', 1),
    (2, 1, NULL, '금리 정책', 2),
    (3, 1, 1, '가입금액 검증', 1),
    (4, 1, 2, '거치기간별 금리', 1),
    (5, 1, 2, '우대금리', 2),
    (6, 2, NULL, '우대금리 이벤트', 1);

-- 6. 테스트케이스 (모두 프로젝트 1 소유, 규칙기반 RULE / RAG / LLM 출처 혼합, author NULL = 시스템·AI 생성)
--    folder: 3 가입금액 검증(1~4, 10, 14) / 4 거치기간별 금리(5~7, 11) / 5 우대금리(8, 9)
--    TC 1~4(개별 경계값)는 파라미터화 TC-114(데이터셋 4행)로 대체 → DEPRECATED (이력·결함 연결 보존)
INSERT IGNORE INTO test_case
    (id, tc_code, project_id, folder_id, title, module, priority, status, source, technique, review_status, origin_project_id) VALUES
    (1,  'TC-101', 1, 3, '가입금액 경계값 - 최소금액 미만(9,999원)',   '가입금액', 'HIGH',   'DEPRECATED', 'RULE', 'BOUNDARY_VALUE',        'APPROVED', NULL),
    (2,  'TC-102', 1, 3, '가입금액 경계값 - 최소금액(10,000원)',       '가입금액', 'HIGH',   'DEPRECATED', 'RULE', 'BOUNDARY_VALUE',        'APPROVED', NULL),
    (3,  'TC-103', 1, 3, '가입금액 경계값 - 최대금액(3,000,000원)',    '가입금액', 'HIGH',   'DEPRECATED', 'RULE', 'BOUNDARY_VALUE',        'APPROVED', NULL),
    (4,  'TC-104', 1, 3, '가입금액 경계값 - 최대금액 초과(3,000,001원)', '가입금액', 'HIGH',   'DEPRECATED', 'RULE', 'BOUNDARY_VALUE',        'DRAFT',    NULL),
    (5,  'TC-105', 1, 4, '거치기간 1년 선택 시 금리 2.5% 적용',          '금리',     'MEDIUM', 'ACTIVE', 'RULE', 'DECISION_TABLE',        'APPROVED', NULL),
    (6,  'TC-106', 1, 4, '거치기간 2년 선택 시 금리 3.2% 적용',          '금리',     'MEDIUM', 'ACTIVE', 'RULE', 'DECISION_TABLE',        'APPROVED', NULL),
    (7,  'TC-107', 1, 4, '거치기간 3년 선택 시 금리 3.9% 적용',          '금리',     'MEDIUM', 'ACTIVE', 'RULE', 'DECISION_TABLE',        'DRAFT',    NULL),
    (8,  'TC-108', 1, 5, '[과거사례 참고] 기존 고객 우대금리 적용 여부 확인', '우대금리', 'MEDIUM', 'ACTIVE', 'RAG', 'EQUIVALENCE_PARTITION', 'DRAFT', 2),
    (9,  'TC-109', 1, 5, '[과거사례 참고] 신규 고객은 우대금리 미적용',  '우대금리', 'MEDIUM', 'ACTIVE', 'RAG',  'EQUIVALENCE_PARTITION', 'DRAFT',    2),
    (10, 'TC-110', 1, 3, '[AI 생성] 최대 가입금액 + 최장 거치기간 동시 적용 시 총 이자 계산 정합성', '이자계산', 'MEDIUM', 'ACTIVE', 'LLM', 'EXPLORATORY', 'DRAFT', NULL),
    (11, 'TC-111', 1, 4, '[AI 생성] 금리 구간 경계에서 소수점 자릿수 처리 확인', '이자계산', 'LOW', 'ACTIVE', 'LLM', 'EXPLORATORY', 'REJECTED', NULL),
    -- 과거 프로젝트(2) 승인 TC — '다른 프로젝트에서 가져오기'·RAG 검색 대상
    (12, 'TC-112', 2, 6, '갈아타기 기존 고객 우대금리 0.2%p 가산',        '우대금리', 'HIGH',   'ACTIVE', 'MANUAL', 'EQUIVALENCE_PARTITION', 'APPROVED', NULL),
    (13, 'TC-113', 2, 6, '이벤트 시작일 기준 30일 초과 신청 시 우대 미적용', '우대금리', 'MEDIUM', 'ACTIVE', 'MANUAL', 'BOUNDARY_VALUE',        'APPROVED', NULL);

-- 6-2. 파라미터화 TC (docs/08) — 단계 텍스트의 {amount}, {expected}를 데이터셋 행 값으로 치환
INSERT IGNORE INTO test_case
    (id, tc_code, project_id, folder_id, title, module, priority, status, source, technique, review_status, is_parameterized) VALUES
    (14, 'TC-114', 1, 3, '가입금액 경계값 검증 (데이터 기반)', '가입금액', 'HIGH', 'ACTIVE', 'RULE', 'BOUNDARY_VALUE', 'APPROVED', TRUE);

INSERT IGNORE INTO test_case_dataset (id, test_case_id, row_label, param_values, expected_result_override, sort_order) VALUES
    (1, 14, '최소금액 미만(9,999원)',     '{"amount": 9999}',    '"최소 가입금액은 10,000원입니다" 오류, 가입 실패', 1),
    (2, 14, '최소금액(10,000원)',         '{"amount": 10000}',   '가입 정상 처리', 2),
    (3, 14, '최대금액(3,000,000원)',      '{"amount": 3000000}', '가입 정상 처리', 3),
    (4, 14, '최대금액 초과(3,000,001원)', '{"amount": 3000001}', '"최대 가입금액은 3,000,000원입니다" 오류, 가입 실패', 4);

-- 6-1. TC ↔ 원자 요구사항 다대다 링크 (docs/07). TC 10은 금액 범위 + 거치기간 두 요구사항을 함께 검증.
--      TC 8·9(RAG, 프로젝트 1)는 다른 프로젝트(2) 요구사항이라 링크하지 않음 — 참고 출처는 origin_project_id로 표시
INSERT IGNORE INTO test_case_requirement_link (id, test_case_id, atomic_requirement_id) VALUES
    (1, 1, 1), (2, 2, 1), (3, 3, 1), (4, 4, 1),
    (5, 5, 3), (6, 6, 3), (7, 7, 3),
    (8, 10, 1), (9, 10, 3),
    (10, 11, 2),
    (11, 12, 4), (12, 13, 4),
    (13, 14, 1);

-- 원본의 steps 텍스트("1. ...\n2. ...")를 단계 행으로 분리, 기대결과는 마지막 단계에 기재
INSERT IGNORE INTO test_step (id, test_case_id, step_no, action, expected_result) VALUES
    (1,  1, 1, '가입금액에 9,999원 입력', NULL),
    (2,  1, 2, '가입 신청', '"최소 가입금액은 10,000원입니다" 오류 메시지 표시, 가입 실패'),
    (3,  2, 1, '가입금액에 10,000원 입력', NULL),
    (4,  2, 2, '가입 신청', '가입 정상 처리'),
    (5,  3, 1, '가입금액에 3,000,000원 입력', NULL),
    (6,  3, 2, '가입 신청', '가입 정상 처리'),
    (7,  4, 1, '가입금액에 3,000,001원 입력', NULL),
    (8,  4, 2, '가입 신청', '"최대 가입금액은 3,000,000원입니다" 오류 메시지 표시, 가입 실패'),
    (9,  5, 1, '거치기간 "1년" 선택', NULL),
    (10, 5, 2, '상품 가입 진행', '적용 금리 2.5%로 계약서/앱 화면에 표시'),
    (11, 6, 1, '거치기간 "2년" 선택', NULL),
    (12, 6, 2, '상품 가입 진행', '적용 금리 3.2%로 계약서/앱 화면에 표시'),
    (13, 7, 1, '거치기간 "3년" 선택', NULL),
    (14, 7, 2, '상품 가입 진행', '적용 금리 3.9%로 계약서/앱 화면에 표시'),
    (15, 8, 1, '기존 고객 계정으로 로그인', NULL),
    (16, 8, 2, '신규 상품 가입 진행', NULL),
    (17, 8, 3, '최종 적용 금리 확인', '기본 금리 + 0.2%p 우대금리가 합산되어 표시됨 (과거 갈아타기 이벤트 유사 케이스)'),
    (18, 9, 1, '신규 고객 계정으로 로그인', NULL),
    (19, 9, 2, '신규 상품 가입 진행', NULL),
    (20, 9, 3, '최종 적용 금리 확인', '기본 금리만 적용, 우대금리 미반영 (과거 갈아타기 이벤트 유사 케이스)'),
    (21, 10, 1, '가입금액 3,000,000원, 거치기간 3년 선택', NULL),
    (22, 10, 2, '가입 신청', NULL),
    (23, 10, 3, '예상 만기 이자 확인', '금리 3.9% 기준으로 계산된 예상 이자 금액이 정확히 표시되고, 소수점 처리(원단위 절사)가 규정대로 적용됨'),
    (24, 11, 1, '거치기간 1년 선택(금리 2.5%)', NULL),
    (25, 11, 2, '가입금액 10,000원 입력', NULL),
    (26, 11, 3, '예상 이자 확인', '이자 계산 시 소수점 둘째자리까지 정확히 계산되고 반올림/절사 규정에 맞게 표시됨'),
    (27, 12, 1, '기존 적금 보유 고객 계정으로 로그인', NULL),
    (28, 12, 2, '자유적금 갈아타기 신청', NULL),
    (29, 12, 3, '적용 금리 확인', '기본 금리 + 0.2%p 가산 금리 표시'),
    (30, 13, 1, '이벤트 시작일 + 31일 시점으로 시스템 일자 설정', NULL),
    (31, 13, 2, '갈아타기 신청', '"신청 기간이 지났습니다" 안내, 우대금리 미적용'),
    (32, 14, 1, '가입금액에 {amount}원 입력', NULL),
    (33, 14, 2, '가입 신청', '{expected}');

-- 7. 테스트 차수 (원본 test_round)
INSERT IGNORE INTO test_cycle (id, project_id, cycle_no, name, start_date, end_date, status) VALUES
    (1, 1, 1, '1차 통합테스트', DATE '2026-09-20', DATE '2026-09-30', 'IN_PROGRESS');

-- 8. 차수별 수행 항목 (원본 test_round_case: success/fail/block/not_executed → PASS/FAIL/BLOCKED/NOT_RUN)
--    미수행 항목은 qa.kim 담당으로 배정 → 대시보드 '내 할일'에 노출
INSERT IGNORE INTO test_execution (id, cycle_id, test_case_id, tc_version, assignee_id, result, executed_by, executed_at) VALUES
    (1, 1, 1, 1, 1, 'PASS',    1,    TIMESTAMP '2026-09-22 10:15:00'),
    (2, 1, 2, 1, 1, 'PASS',    1,    TIMESTAMP '2026-09-22 10:20:00'),
    (3, 1, 3, 1, 1, 'FAIL',    1,    TIMESTAMP '2026-09-22 11:00:00'),
    (4, 1, 4, 1, 1, 'BLOCKED', 1,    TIMESTAMP '2026-09-22 11:10:00'),
    (5, 1, 5, 1, 3, 'PASS',    3,    TIMESTAMP '2026-09-23 09:30:00'),
    (6, 1, 6, 1, 3, 'PASS',    3,    TIMESTAMP '2026-09-23 09:40:00'),
    (7, 1, 7, 1, 1, 'NOT_RUN', NULL, NULL),
    (8, 1, 8, 1, 1, 'NOT_RUN', NULL, NULL),
    (9, 1, 9, 1, 1, 'NOT_RUN', NULL, NULL);

-- 파라미터화 TC-114: 데이터셋 행마다 개별 실행 결과 (docs/08)
INSERT IGNORE INTO test_execution (id, cycle_id, test_case_id, dataset_id, tc_version, assignee_id, result, executed_by, executed_at) VALUES
    (10, 1, 14, 1, 1, 1, 'PASS', 1, TIMESTAMP '2026-09-24 10:00:00'),
    (11, 1, 14, 2, 1, 1, 'PASS', 1, TIMESTAMP '2026-09-24 10:05:00'),
    (12, 1, 14, 3, 1, 1, 'FAIL', 1, TIMESTAMP '2026-09-24 10:10:00'),
    (13, 1, 14, 4, 1, 1, 'PASS', 1, TIMESTAMP '2026-09-24 10:15:00');

INSERT IGNORE INTO test_execution_history (id, execution_id, result, comment, executed_by, executed_at) VALUES
    (1, 1, 'PASS',    NULL, 1, TIMESTAMP '2026-09-22 10:15:00'),
    (2, 2, 'PASS',    NULL, 1, TIMESTAMP '2026-09-22 10:20:00'),
    (3, 3, 'FAIL',    '3,000,000원 입력 시 가입이 거부됨. 결함 등록함', 1, TIMESTAMP '2026-09-22 11:00:00'),
    (4, 4, 'BLOCKED', '3번 케이스 결함으로 인해 후속 검증 불가', 1, TIMESTAMP '2026-09-22 11:10:00'),
    (5, 5, 'PASS',    NULL, 3, TIMESTAMP '2026-09-23 09:30:00'),
    (6, 6, 'PASS',    NULL, 3, TIMESTAMP '2026-09-23 09:40:00'),
    (7, 10, 'PASS',   NULL, 1, TIMESTAMP '2026-09-24 10:00:00'),
    (8, 11, 'PASS',   NULL, 1, TIMESTAMP '2026-09-24 10:05:00'),
    (9, 12, 'FAIL',   '기존 결함(DF-0001)과 동일 증상 재현', 1, TIMESTAMP '2026-09-24 10:10:00'),
    (10, 13, 'PASS',  NULL, 1, TIMESTAMP '2026-09-24 10:15:00');

-- 9. 결함 (원본 severity high → MAJOR, status open → OPEN)
INSERT IGNORE INTO defect (id, project_id, defect_code, title, description, severity, priority, status, reporter_id, assignee_id, execution_id) VALUES
    (1, 1, 'DF-0001', '최대 가입금액(300만원) 입력 시 정상 가입 처리가 거부됨',
        '경계값인 3,000,000원을 입력하면 시스템이 초과금액으로 오인하여 가입을 거부함. min/max 비교 로직에서 부등호가 잘못된 것으로 추정(<= 이어야 하는데 < 로 구현된 듯).',
        'MAJOR', 'HIGH', 'OPEN', 1, 2, 3);

-- ─────────────────────────────────────────────────────────────
-- 10. '공통 테스트케이스' 마스터 프로젝트 — 여러 프로젝트가 반복적으로 필요로 하는 표준/회귀 테스트 세트.
--     프로젝트 멤버는 두지 않음(과거 프로젝트 KB-SWITCH와 같은 패턴) — '다른 프로젝트에서 가져오기'로만 사용.
INSERT IGNORE INTO project (id, code, name, description, status) VALUES
    (3, 'COMMON-TC', '공통 테스트케이스',
        '여러 프로젝트에서 재사용하는 표준 테스트 세트. 프로젝트별로 ''다른 프로젝트에서 가져오기''로 복사해서 사용.',
        'ACTIVE');

INSERT IGNORE INTO test_case_folder (id, project_id, parent_folder_id, name, sort_order) VALUES
    (7,  3, NULL, '화면입력 공통검증', 1),
    (8,  3, NULL, '인증/보안 공통검증', 2),
    (9,  3, NULL, '이체·거래 공통검증', 3),
    (10, 3, NULL, '금액 테스트', 4),
    (11, 3, NULL, '금리 테스트', 5),
    (12, 3, NULL, '채널 호환성 - 멀티브라우저', 6),
    (13, 3, NULL, '채널 호환성 - iOS/AOS', 7);

INSERT IGNORE INTO test_case
    (id, tc_code, project_id, folder_id, title, module, priority, status, source, technique, review_status) VALUES
    -- 화면입력 공통검증 (7)
    (15, 'TC-115', 3, 7, '필수값 미입력 시 오류 메시지 노출 확인',        '공통UI', 'MEDIUM', 'ACTIVE', 'MANUAL', 'EQUIVALENCE_PARTITION', 'APPROVED'),
    (16, 'TC-116', 3, 7, '버튼 중복클릭 방지 확인',                     '공통UI', 'MEDIUM', 'ACTIVE', 'MANUAL', 'EXPLORATORY',           'APPROVED'),
    (17, 'TC-117', 3, 7, '비활성화(disabled) 버튼 클릭 무반응 확인',      '공통UI', 'MEDIUM', 'ACTIVE', 'MANUAL', 'EXPLORATORY',           'APPROVED'),
    (18, 'TC-118', 3, 7, '아이디/이메일 대소문자 구분 없이 처리 확인',     '공통UI', 'MEDIUM', 'ACTIVE', 'MANUAL', 'EQUIVALENCE_PARTITION', 'APPROVED'),
    (19, 'TC-119', 3, 7, '입력 최대 길이 초과 시 처리 확인',             '공통UI', 'MEDIUM', 'ACTIVE', 'MANUAL', 'EQUIVALENCE_PARTITION', 'APPROVED'),
    (20, 'TC-120', 3, 7, '숫자 전용 필드에 문자 입력 시 처리 확인',       '공통UI', 'MEDIUM', 'ACTIVE', 'MANUAL', 'EQUIVALENCE_PARTITION', 'APPROVED'),
    (21, 'TC-121', 3, 7, '특수문자/SQL 인젝션 방어 확인',                '공통UI', 'HIGH',   'ACTIVE', 'MANUAL', 'EXPLORATORY',           'APPROVED'),
    (22, 'TC-122', 3, 7, '새로고침/뒤로가기 시 입력값 처리 확인',         '공통UI', 'MEDIUM', 'ACTIVE', 'MANUAL', 'EXPLORATORY',           'APPROVED'),
    -- 인증/보안 공통검증 (8) — 은행권 특화
    (23, 'TC-123', 3, 8, '비밀번호 N회 연속 오류 시 계정 잠금 확인',       '인증', 'HIGH', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (24, 'TC-124', 3, 8, '세션 타임아웃 후 재인증 요구 확인',             '인증', 'HIGH', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (25, 'TC-125', 3, 8, '로그아웃 후 뒤로가기 시 인증 화면으로 이동 확인', '인증', 'HIGH', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (26, 'TC-126', 3, 8, '중요 거래(이체 등) 시 2차 인증(OTP 등) 요구 확인', '인증', 'HIGH', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    -- 이체·거래 공통검증 (9) — 은행권 특화
    (27, 'TC-127', 3, 9, '1일 이체한도 초과 시 처리 확인',               '이체', 'HIGH', 'ACTIVE', 'MANUAL', 'BOUNDARY_VALUE',        'APPROVED'),
    (28, 'TC-128', 3, 9, '잔액 부족 시 이체 실패 처리 확인',             '이체', 'HIGH', 'ACTIVE', 'MANUAL', 'BOUNDARY_VALUE',        'APPROVED'),
    (29, 'TC-129', 3, 9, '동일 계좌 간 이체 방지 확인',                  '이체', 'HIGH', 'ACTIVE', 'MANUAL', 'EQUIVALENCE_PARTITION', 'APPROVED'),
    (30, 'TC-130', 3, 9, '이체 처리 중 네트워크 단절 시 중복이체 방지 확인', '이체', 'HIGH', 'ACTIVE', 'MANUAL', 'EQUIVALENCE_PARTITION', 'APPROVED'),
    -- 채널 호환성 - 멀티브라우저 (12)
    (32, 'TC-132', 3, 12, 'Chrome 최신버전 정상 동작 확인',              '브라우저호환성', 'LOW', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (33, 'TC-133', 3, 12, 'Safari 최신버전 정상 동작 확인',              '브라우저호환성', 'LOW', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (34, 'TC-134', 3, 12, 'Edge 최신버전 정상 동작 확인',                '브라우저호환성', 'LOW', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (35, 'TC-135', 3, 12, '구형 브라우저(IE11 등) 미지원 안내 노출 확인', '브라우저호환성', 'LOW', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    -- 채널 호환성 - iOS/AOS (13)
    (36, 'TC-136', 3, 13, 'iOS 최신버전 앱 설치 및 로그인 확인',         '앱호환성', 'LOW', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (37, 'TC-137', 3, 13, 'Android 최신버전 앱 설치 및 로그인 확인',     '앱호환성', 'LOW', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (38, 'TC-138', 3, 13, 'iOS 푸시 알림 수신 확인',                    '앱호환성', 'LOW', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (39, 'TC-139', 3, 13, 'Android 푸시 알림 수신 확인',                '앱호환성', 'LOW', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED'),
    (40, 'TC-140', 3, 13, '화면 회전(가로/세로) 시 레이아웃 깨짐 확인',   '앱호환성', 'LOW', 'ACTIVE', 'MANUAL', 'EXPLORATORY', 'APPROVED');

-- 금액/금리 테스트는 프로젝트별로 필드명·min/max·조건-금리 매핑이 다르므로 사전조건에 '복사 후 수정' 안내를 남김
INSERT IGNORE INTO test_case
    (id, tc_code, project_id, folder_id, title, module, precondition, priority, status, source, technique, review_status, is_parameterized) VALUES
    (31, 'TC-131', 3, 10, '금액 필드 경계값 표준 검증', '금액',
        '템플릿 TC입니다 — 복사해간 프로젝트에서 {필드명}과 데이터셋의 min/max 값을 실제 조건에 맞게 수정해서 사용하세요.',
        'MEDIUM', 'ACTIVE', 'MANUAL', 'BOUNDARY_VALUE', 'APPROVED', TRUE),
    (41, 'TC-141', 3, 11, '금리 구간별 적용값 표준 검증', '금리',
        '템플릿 TC입니다 — 복사해간 프로젝트의 조건-금리 매핑표에 맞게 {조건필드}와 기대 금리값을 수정해서 사용하세요.',
        'MEDIUM', 'ACTIVE', 'MANUAL', 'DECISION_TABLE', 'APPROVED', FALSE);

INSERT IGNORE INTO test_case_dataset (id, test_case_id, row_label, param_values, expected_result_override, sort_order) VALUES
    (5, 31, '최소값-1 (예시 999원) — 실패',      '{"amount": 999}',    '허용 범위 미만 — 오류 안내, 처리 거부 (실제 min 값으로 수정 필요)', 1),
    (6, 31, '최소값 (예시 1,000원) — 성공',      '{"amount": 1000}',   '정상 처리 (실제 min 값으로 수정 필요)', 2),
    (7, 31, '최대값 (예시 100,000원) — 성공',    '{"amount": 100000}', '정상 처리 (실제 max 값으로 수정 필요)', 3),
    (8, 31, '최대값+1 (예시 100,001원) — 실패', '{"amount": 100001}', '허용 범위 초과 — 오류 안내, 처리 거부 (실제 max 값으로 수정 필요)', 4);

INSERT IGNORE INTO test_step (id, test_case_id, step_no, action, expected_result) VALUES
    (34, 15, 1, '필수 입력 필드를 비운 채 저장/제출', NULL),
    (35, 15, 2, '결과 확인', '비어 있는 각 필드마다 해당 필드명을 포함한 오류 메시지가 정확히 노출되고 제출이 차단됨'),
    (36, 16, 1, '저장/제출 버튼을 빠르게 연속 클릭', NULL),
    (37, 16, 2, '결과 확인', '요청이 한 번만 처리되고 중복 데이터/중복 요청이 생성되지 않음(버튼 비활성화 또는 요청 잠금 동작)'),
    (38, 17, 1, '필수 조건을 충족하지 않은 상태로 화면 진입', NULL),
    (39, 17, 2, '비활성화된 버튼을 클릭', '버튼이 비활성화 상태를 유지하며 클릭해도 아무 동작이 일어나지 않음'),
    (40, 18, 1, '대문자를 포함한 아이디/이메일로 가입', NULL),
    (41, 18, 2, '소문자로 변환한 값으로 로그인 시도', '동일 계정으로 인식되어 정상 로그인됨(또는 정책에 따라 중복 가입이 차단됨)'),
    (42, 19, 1, '필드별 정의된 최대 글자수를 초과하여 입력', NULL),
    (43, 19, 2, '저장/제출', '초과 입력이 자동으로 잘리거나, 길이 초과 오류 메시지가 노출되고 저장되지 않음'),
    (44, 20, 1, '금액/전화번호 등 숫자 전용 필드에 문자를 입력', NULL),
    (45, 20, 2, '결과 확인', '문자 입력 자체가 막히거나, 입력 시 오류 메시지가 노출되고 숫자 외 값은 저장되지 않음'),
    (46, 21, 1, '입력 필드에 '' OR 1=1 -- 등 SQL 인젝션 구문을 입력하여 제출', NULL),
    (47, 21, 2, '결과 확인', '입력값이 그대로 실행되지 않고 이스케이프 처리되거나 오류로 차단되며, 비정상적인 데이터 노출/조작이 발생하지 않음'),
    (48, 22, 1, '폼 작성 중 브라우저 새로고침 또는 뒤로가기 수행', NULL),
    (49, 22, 2, '결과 확인', '정책에 정의된 대로 임시저장된 값이 복원되거나, 입력값이 초기화되고 재작성을 요구함(임의로 이전 값이 부분 반영되지 않음)'),

    (50, 23, 1, '정책에 정의된 허용 횟수를 초과하여 잘못된 비밀번호 입력', NULL),
    (51, 23, 2, '결과 확인', '계정이 잠기고 ''비밀번호 오류 횟수 초과로 잠금 처리되었습니다'' 등 안내 메시지가 노출됨'),
    (52, 24, 1, '로그인 후 정책에 정의된 세션 유지시간 동안 아무 조작 없이 대기', NULL),
    (53, 24, 2, '대기 후 임의 화면/기능 조작 시도', '세션이 만료되어 재로그인 화면으로 이동하거나 재인증을 요구함'),
    (54, 25, 1, '로그인 후 로그아웃 처리', NULL),
    (55, 25, 2, '브라우저 뒤로가기로 이전(로그인 필요) 화면 접근 시도', '이전 화면이 그대로 노출되지 않고 로그인 화면으로 이동하거나 재인증을 요구함'),
    (56, 26, 1, '단순 조회 기능 수행', '추가 인증 없이 정상 조회됨'),
    (57, 26, 2, '이체 등 자금이동 거래 진행', 'OTP/보안카드 등 2차 인증 단계가 강제되고, 인증 완료 전에는 거래가 처리되지 않음'),

    (58, 27, 1, '1일 이체한도를 초과하는 금액으로 이체 시도', NULL),
    (59, 27, 2, '결과 확인', '한도 초과 오류 메시지가 노출되고 거래가 차단됨'),
    (60, 28, 1, '출금계좌 잔액보다 큰 금액으로 이체 시도', NULL),
    (61, 28, 2, '결과 확인', '잔액 부족 오류 메시지가 정확히 노출되고 거래가 실패 처리됨'),
    (62, 29, 1, '출금계좌와 입금계좌를 동일하게 설정하여 이체 시도', NULL),
    (63, 29, 2, '결과 확인', '동일 계좌 간 이체는 허용되지 않는다는 안내와 함께 거래가 차단됨'),
    (64, 30, 1, '이체 요청 처리 중 네트워크 연결을 강제로 끊음', NULL),
    (65, 30, 2, '재연결 후 동일 요청을 재시도', '출금이 중복으로 발생하지 않고(멱등성 보장) 원거래의 성공/실패 여부가 정확히 조회됨'),

    (66, 31, 1, '{필드명}에 {amount}원 입력', NULL),
    (67, 31, 2, '저장/등록 진행', '{expected}'),
    (68, 41, 1, '{조건필드} 선택', NULL),
    (69, 41, 2, '적용 금리 확인', '각 조건에 매핑된 금리가 정확히 표시되는지 확인 (프로젝트별 조건-금리 매핑표 참고)'),

    (70, 32, 1, 'Chrome 최신 버전으로 접속하여 주요 기능(로그인/조회/거래 등) 수행', NULL),
    (71, 32, 2, '결과 확인', '레이아웃 깨짐이나 기능 오류 없이 정상 동작함'),
    (72, 33, 1, 'Safari 최신 버전으로 접속하여 주요 기능(로그인/조회/거래 등) 수행', NULL),
    (73, 33, 2, '결과 확인', '레이아웃 깨짐이나 기능 오류 없이 정상 동작함'),
    (74, 34, 1, 'Edge 최신 버전으로 접속하여 주요 기능(로그인/조회/거래 등) 수행', NULL),
    (75, 34, 2, '결과 확인', '레이아웃 깨짐이나 기능 오류 없이 정상 동작함'),
    (76, 35, 1, 'IE11 등 미지원 브라우저로 접속', NULL),
    (77, 35, 2, '결과 확인', '서비스 이용이 제한되며 ''지원되지 않는 브라우저입니다'' 등 안내 메시지가 노출됨'),

    (78, 36, 1, 'iOS 최신 버전 기기에 앱 설치', NULL),
    (79, 36, 2, '로그인 진행', '설치 및 로그인이 정상적으로 완료됨'),
    (80, 37, 1, 'Android 최신 버전 기기에 앱 설치', NULL),
    (81, 37, 2, '로그인 진행', '설치 및 로그인이 정상적으로 완료됨'),
    (82, 38, 1, '푸시 알림이 발생하는 이벤트(거래 알림 등)를 트리거', NULL),
    (83, 38, 2, '결과 확인', 'iOS 기기에 푸시 알림이 정상적으로 수신됨'),
    (84, 39, 1, '푸시 알림이 발생하는 이벤트(거래 알림 등)를 트리거', NULL),
    (85, 39, 2, '결과 확인', 'Android 기기에 푸시 알림이 정상적으로 수신됨'),
    (86, 40, 1, '화면을 세로에서 가로로(또는 반대로) 회전', NULL),
    (87, 40, 2, '결과 확인', '레이아웃이 깨지지 않고 화면 비율에 맞게 정상적으로 재배치됨');
