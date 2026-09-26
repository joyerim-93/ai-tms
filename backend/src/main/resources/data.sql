-- 초기 샘플 데이터 — 기동마다 실행되므로 MERGE(KEY)로 멱등 처리.
-- 인증 도입 전까지 "로그인 사용자"는 id=1(qa01)로 가정.

MERGE INTO users (id, login_id, name, email, role) KEY (id) VALUES
    (1, 'qa01',    '김큐에이', 'qa01@aitms.local',    'QA'),
    (2, 'dev01',   '이개발',   'dev01@aitms.local',   'DEV'),
    (3, 'dev02',   '박개발',   'dev02@aitms.local',   'DEV'),
    (4, 'biz01',   '최현업',   'biz01@aitms.local',   'BIZ'),
    (5, 'admin',   '관리자',   'admin@aitms.local',   'ADMIN');

MERGE INTO project (id, code, name, description, status, start_date, end_date) KEY (id) VALUES
    (1, 'PRJ-DEMO', '데모 프로젝트', 'AI-TMS 샘플 프로젝트', 'ACTIVE', DATE '2026-09-01', DATE '2026-12-31');

MERGE INTO project_member (project_id, user_id, project_role) KEY (project_id, user_id) VALUES
    (1, 1, 'QA'),
    (1, 2, 'DEV'),
    (1, 3, 'DEV'),
    (1, 4, 'BIZ'),
    (1, 5, 'PM');
