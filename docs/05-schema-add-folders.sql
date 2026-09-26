-- 기존 01-schema.sql에 추가되는 부분
-- 프로젝트 아래 디렉토리(폴더) 구조 + test_case에 folder_id 연결

CREATE TABLE IF NOT EXISTS test_case_folder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    parent_folder_id BIGINT,          -- NULL이면 프로젝트 최상위 폴더
    name VARCHAR(200) NOT NULL,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id),
    FOREIGN KEY (parent_folder_id) REFERENCES test_case_folder(id)
);

-- test_case 테이블에 folder_id 컬럼 추가 (01-schema.sql의 test_case CREATE TABLE에 반영)
ALTER TABLE test_case ADD COLUMN IF NOT EXISTS folder_id BIGINT;
ALTER TABLE test_case ADD CONSTRAINT IF NOT EXISTS fk_test_case_folder
    FOREIGN KEY (folder_id) REFERENCES test_case_folder(id);

-- 예시 데이터: KB 적금 통장 신설 프로젝트(id=1) 아래 폴더 구조
INSERT INTO test_case_folder (id, project_id, parent_folder_id, name, sort_order) VALUES
  (1, 1, NULL, '가입 프로세스', 1),
  (2, 1, NULL, '금리 정책', 2),
  (3, 1, 1, '가입금액 검증', 1),
  (4, 1, 2, '거치기간별 금리', 1),
  (5, 1, 2, '우대금리', 2);

-- 기존 02-sample-data.sql의 test_case에 folder_id 매핑 예시 (UPDATE로 반영)
UPDATE test_case SET folder_id = 3 WHERE id IN (1, 2, 3, 4, 10);   -- 가입금액 관련
UPDATE test_case SET folder_id = 4 WHERE id IN (5, 6, 7, 11);      -- 거치기간별 금리
UPDATE test_case SET folder_id = 5 WHERE id IN (8, 9);             -- 우대금리(RAG 추천분)
