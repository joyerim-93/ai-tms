-- ai-tms 예시 데이터
-- backend/src/main/resources/data.sql 로 사용
-- 시나리오: KB 적금 통장 신설 프로젝트 + 유사 과거 프로젝트(적금 갈아타기) 1개

-- 1. 프로젝트
INSERT INTO project (id, name, description) VALUES
  (1, 'KB 적금 통장 신설', '신규 적금 상품 출시 프로젝트. 금리 2.5~3.9%, 거치기간 1/2/3년, 가입금액 1만~300만원'),
  (2, 'KB 자유적금 갈아타기 이벤트', '기존 적금 고객 대상 우대금리 갈아타기 이벤트 (과거 프로젝트, RAG 검색 대상)');

-- 2. 요구사항 (원문)
INSERT INTO requirement (id, project_id, raw_text, source) VALUES
  (1, 1, 'KB 적금 통장 신설. 금리는 2.5~3.9%이고, 금리에 영향을 주는 것은 거치기간이 1년/2년/3년이냐에 따라 다름. 가입금액은 최소 1만원, 최대 300만원.', 'manual'),
  (2, 2, '자유적금 갈아타기 시 기존 고객은 우대금리 0.2%p 추가 제공. 갈아타기 신청 기간은 이벤트 시작일로부터 30일 이내.', 'manual');

-- 3. 원자 요구사항 (AI 에이전트가 분해한 결과 예시)
INSERT INTO atomic_requirement (id, requirement_id, atomic_text, type, min_value, max_value, unit, conditions) VALUES
  (1, 1, '가입금액은 최소 1만원, 최대 300만원이다', 'amount_range', 10000, 3000000, 'KRW', NULL),
  (2, 1, '금리는 2.5%에서 3.9% 사이이다', 'rate_range', 2.5, 3.9, 'percent', NULL),
  (3, 1, '거치기간(1년/2년/3년)에 따라 적용 금리가 다르다', 'period_condition', NULL, NULL, 'year', '{"options":["1년","2년","3년"],"rate_map":{"1년":2.5,"2년":3.2,"3년":3.9}}'),
  (4, 2, '기존 고객 갈아타기 시 우대금리 0.2%p가 추가된다', 'boolean_flag', NULL, NULL, NULL, '{"flag":"existing_customer","bonus_rate":0.2}');

-- 4. 규칙 카탈로그 (타입별 정형 테스트 기법 템플릿, 초기 4개 타입)
INSERT INTO rule_catalog (id, requirement_type, technique, template) VALUES
  (1, 'amount_range', 'boundary_value', '경계값 테스트: {min}-1(실패), {min}(성공), {min}+1(성공), {max}-1(성공), {max}(성공), {max}+1(실패)'),
  (2, 'rate_range', 'boundary_value', '경계값 테스트: {min}% 미만(적용불가), {min}%(최소금리 적용), {max}%(최대금리 적용), {max}% 초과(적용불가)'),
  (3, 'period_condition', 'decision_table', '조건별 디시전테이블: 거치기간 옵션 각각에 대해 매핑된 금리가 정확히 적용되는지 검증'),
  (4, 'boolean_flag', 'equivalence_partition', '동등분할: 플래그 true(우대금리 적용됨), 플래그 false(우대금리 미적용)');

-- 5. 테스트케이스 (규칙기반 / RAG / LLM 세 출처를 섞어서 예시로 구성)

-- 5-1. 규칙기반(rule) 추천 결과 - 가입금액 경계값
INSERT INTO test_case (id, atomic_requirement_id, project_id, title, steps, expected_result, technique, source, status, created_by) VALUES
  (1, 1, 1, '가입금액 경계값 - 최소금액 미만(9,999원)', '1. 가입금액에 9,999원 입력\n2. 가입 신청', '"최소 가입금액은 10,000원입니다" 오류 메시지 표시, 가입 실패', 'boundary_value', 'rule', 'approved', 'system'),
  (2, 1, 1, '가입금액 경계값 - 최소금액(10,000원)', '1. 가입금액에 10,000원 입력\n2. 가입 신청', '가입 정상 처리', 'boundary_value', 'rule', 'approved', 'system'),
  (3, 1, 1, '가입금액 경계값 - 최대금액(3,000,000원)', '1. 가입금액에 3,000,000원 입력\n2. 가입 신청', '가입 정상 처리', 'boundary_value', 'rule', 'approved', 'system'),
  (4, 1, 1, '가입금액 경계값 - 최대금액 초과(3,000,001원)', '1. 가입금액에 3,000,001원 입력\n2. 가입 신청', '"최대 가입금액은 3,000,000원입니다" 오류 메시지 표시, 가입 실패', 'boundary_value', 'rule', 'draft', 'system');

-- 5-2. 규칙기반(rule) 추천 결과 - 거치기간별 금리 디시전테이블
INSERT INTO test_case (id, atomic_requirement_id, project_id, title, steps, expected_result, technique, source, status, created_by) VALUES
  (5, 3, 1, '거치기간 1년 선택 시 금리 2.5% 적용', '1. 거치기간 "1년" 선택\n2. 상품 가입 진행', '적용 금리 2.5%로 계약서/앱 화면에 표시', 'decision_table', 'rule', 'approved', 'system'),
  (6, 3, 1, '거치기간 2년 선택 시 금리 3.2% 적용', '1. 거치기간 "2년" 선택\n2. 상품 가입 진행', '적용 금리 3.2%로 계약서/앱 화면에 표시', 'decision_table', 'rule', 'approved', 'system'),
  (7, 3, 1, '거치기간 3년 선택 시 금리 3.9% 적용', '1. 거치기간 "3년" 선택\n2. 상품 가입 진행', '적용 금리 3.9%로 계약서/앱 화면에 표시', 'decision_table', 'rule', 'draft', 'system');

-- 5-3. RAG(rag) 추천 결과 - 과거 프로젝트(자유적금 갈아타기)의 우대금리 테스트케이스를 유사 검색으로 가져온 예시
INSERT INTO test_case (id, atomic_requirement_id, project_id, title, steps, expected_result, technique, source, status, origin_project_id, created_by) VALUES
  (8, 4, 1, '[과거사례 참고] 기존 고객 우대금리 적용 여부 확인', '1. 기존 고객 계정으로 로그인\n2. 신규 상품 가입 진행\n3. 최종 적용 금리 확인', '기본 금리 + 0.2%p 우대금리가 합산되어 표시됨 (과거 갈아타기 이벤트 유사 케이스)', 'equivalence_partition', 'rag', 'draft', 2, 'ai-agent'),
  (9, 4, 1, '[과거사례 참고] 신규 고객은 우대금리 미적용', '1. 신규 고객 계정으로 로그인\n2. 신규 상품 가입 진행\n3. 최종 적용 금리 확인', '기본 금리만 적용, 우대금리 미반영 (과거 갈아타기 이벤트 유사 케이스)', 'equivalence_partition', 'rag', 'draft', 2, 'ai-agent');

-- 5-4. LLM(llm) 신규 생성 결과 - 규칙/RAG로 못 잡은 조합 케이스 예시
INSERT INTO test_case (id, atomic_requirement_id, project_id, title, steps, expected_result, technique, source, status, created_by) VALUES
  (10, 1, 1, '[AI 생성] 최대 가입금액 + 최장 거치기간 동시 적용 시 총 이자 계산 정합성', '1. 가입금액 3,000,000원, 거치기간 3년 선택\n2. 가입 신청\n3. 예상 만기 이자 확인', '금리 3.9% 기준으로 계산된 예상 이자 금액이 정확히 표시되고, 소수점 처리(원단위 절사)가 규정대로 적용됨', 'exploratory', 'llm', 'draft', 'ai-agent'),
  (11, 2, 1, '[AI 생성] 금리 구간 경계에서 소수점 자릿수 처리 확인', '1. 거치기간 1년 선택(금리 2.5%)\n2. 가입금액 10,000원 입력\n3. 예상 이자 확인', '이자 계산 시 소수점 둘째자리까지 정확히 계산되고 반올림/절사 규정에 맞게 표시됨', 'exploratory', 'llm', 'rejected', 'ai-agent');

-- 6. 테스트수행 차수
INSERT INTO test_round (id, project_id, name, start_date, end_date, status) VALUES
  (1, 1, '1차 통합테스트', '2026-09-20', '2026-09-30', 'in_progress');

-- 7. 차수별 테스트케이스 실행 결과 (성공/실패/block/미수행 골고루)
INSERT INTO test_round_case (id, test_round_id, test_case_id, result, executed_by, executed_at, comment) VALUES
  (1, 1, 1, 'success', 'qa.kim', '2026-09-22 10:15:00', NULL),
  (2, 1, 2, 'success', 'qa.kim', '2026-09-22 10:20:00', NULL),
  (3, 1, 3, 'fail', 'qa.kim', '2026-09-22 11:00:00', '3,000,000원 입력 시 가입이 거부됨. 결함 등록함'),
  (4, 1, 4, 'block', 'qa.kim', '2026-09-22 11:10:00', '3번 케이스 결함으로 인해 후속 검증 불가'),
  (5, 1, 5, 'success', 'qa.lee', '2026-09-23 09:30:00', NULL),
  (6, 1, 6, 'success', 'qa.lee', '2026-09-23 09:40:00', NULL),
  (7, 1, 7, 'not_executed', NULL, NULL, NULL),
  (8, 1, 8, 'not_executed', NULL, NULL, NULL),
  (9, 1, 9, 'not_executed', NULL, NULL, NULL);

-- 8. 결함
INSERT INTO defect (id, test_round_case_id, project_id, title, description, severity, status, assignee, reporter) VALUES
  (1, 3, 1, '최대 가입금액(300만원) 입력 시 정상 가입 처리가 거부됨', '경계값인 3,000,000원을 입력하면 시스템이 초과금액으로 오인하여 가입을 거부함. min/max 비교 로직에서 부등호가 잘못된 것으로 추정(<= 이어야 하는데 < 로 구현된 듯).', 'high', 'open', 'dev.park', 'qa.kim');
