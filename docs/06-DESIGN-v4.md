# ai-tms 개념 수정 v4 (프로젝트 전역화 + 테스트케이스 디렉토리 구조)

## 1. 핵심 변경

1. **프로젝트가 전역 컨텍스트가 됩니다.** 헤더에 프로젝트 선택기(드롭다운 또는 pill)를 두고,
   대시보드/테스트케이스/테스트 수행/이슈관리 4개 화면 모두 "지금 선택된 프로젝트" 기준으로 데이터가 필터링됩니다.
   탭을 이동해도 선택된 프로젝트는 유지됩니다 (Pinia store에 currentProjectId 저장).
2. **프로젝트 등록(생성)은 테스트케이스 탭 안에서만** 합니다. 헤더의 선택기는 "기존 프로젝트 중 전환"만 하고,
   "+ 새 프로젝트" 버튼은 테스트케이스 화면에만 있습니다.
3. **테스트케이스는 프로젝트 아래 디렉토리(폴더) 구조로 정리**합니다. 폴더는 다단계 중첩 가능
   (예: "금리 정책" 폴더 아래 "거치기간별 금리", "우대금리" 하위 폴더).

## 2. 레이아웃

```
┌───────────────────────────────────────────────────────────┐
│ AI-TMS   대시보드 테스트케이스 테스트수행 이슈관리   [KB 적금 통장 신설 ▾] │ ← 헤더: 메뉴 + 프로젝트 선택기(우측)
├───────────────────────────────────────────────────────────┤
│ (대시보드/테스트수행/이슈관리 화면은 선택된 프로젝트로 필터링된 데이터만 표시)
```

**테스트케이스 화면만 별도 좌우 분할 레이아웃:**

```
┌───────────────────────────────────────────────────────────┐
│ 테스트케이스                                [+ 새 프로젝트]   │
├───────────────┬───────────────────────────────────────────┤
│ 📁 가입 프로세스│  선택된 폴더의 테스트케이스 목록              │
│  └ 가입금액 검증│  (제목 / 기법 / 출처뱃지 / 상태뱃지)          │
│ 📁 금리 정책    │                                            │
│  ├ 거치기간별   │  [+ 테스트케이스 추가]  [AI 추천 요청]       │
│  └ 우대금리     │                                            │
│ [+ 폴더 추가]   │                                            │
└───────────────┴───────────────────────────────────────────┘
```

- 좌측: 폴더 트리 (프로젝트 선택 시 그 프로젝트의 폴더 트리로 교체됨), 트리 하단에 "+ 폴더 추가"
- 우측: 선택된 폴더에 속한 테스트케이스 리스트. 폴더 선택 안 하면 프로젝트 전체 테스트케이스
- "+ 새 프로젝트"는 이 화면 우측 상단에만 존재

## 3. 데이터 모델 변경 (동봉한 05-schema-add-folders.sql 참고)

- `test_case_folder` 테이블 신규: `project_id`, `parent_folder_id`(자기참조, 중첩 폴더), `name`
- `test_case`에 `folder_id` 컬럼 추가 (어느 폴더에 속하는지)
- AI 에이전트가 추천한 테스트케이스는 일단 프로젝트의 "미분류" 폴더(또는 원자요구사항과 연결된 폴더 추정)에 draft로 들어가고, 사람이 검수하면서 적절한 폴더로 옮기는 흐름으로 확장 가능

## 4. 프론트엔드 구조 변경

```
frontend/src/
├── stores/
│   └── projectStore.js       # currentProjectId, projectList (Pinia, 전역)
├── components/
│   ├── ProjectSelector.vue   # 헤더용, 전환만 가능 (등록 버튼 없음)
│   ├── FolderTree.vue        # 테스트케이스 탭 전용, 재귀 트리 컴포넌트
│   └── StatusBadge.vue
└── views/
    ├── DashboardView.vue     # projectStore.currentProjectId로 API 필터링
    ├── TestCaseRepoView.vue  # FolderTree + 프로젝트 등록/폴더 관리 포함
    ├── TestRunView.vue       # projectStore.currentProjectId로 필터링
    └── DefectView.vue        # projectStore.currentProjectId로 필터링
```

모든 API 호출(대시보드 요약, 테스트케이스 목록, 테스트수행 차수, 결함 목록)에 `projectId` 쿼리 파라미터가 붙습니다.

## 5. REST 엔드포인트 추가/변경

- `GET /api/projects` — 전체 프로젝트 목록 (헤더 선택기용)
- `POST /api/projects` — 프로젝트 등록 (테스트케이스 탭에서만 호출)
- `GET /api/projects/{projectId}/folders` — 폴더 트리 조회
- `POST /api/projects/{projectId}/folders` — 폴더 생성 (parent_folder_id 옵션)
- `GET /api/test-cases?projectId={id}&folderId={id}` — folderId 없으면 프로젝트 전체
- `GET /api/dashboard/summary?projectId={id}`
- `GET /api/test-rounds?projectId={id}`
- `GET /api/defects?projectId={id}`

## 6. Claude Code에 넣을 프롬프트

```
컨셉을 이렇게 수정할게. 동봉한 docs/06-DESIGN-v4.md, docs/05-schema-add-folders.sql 읽어줘.

## 변경사항
1. 프로젝트를 전역 컨텍스트로 만들어줘.
   - 헤더 우측에 프로젝트 선택 드롭다운(ProjectSelector.vue) 추가. 여기서는 "전환만" 가능하고 생성 기능은 없음.
   - Pinia store(projectStore)로 currentProjectId를 전역 관리하고, 탭을 이동해도 유지되게 해줘.
   - 대시보드/테스트수행/이슈관리 화면은 모두 이 currentProjectId로 API를 필터링해서 조회하도록 수정해줘.

2. 프로젝트 등록(생성) 기능은 테스트케이스 화면 안에만 넣어줘. "+ 새 프로젝트" 버튼은 다른 화면엔 없어야 해.

3. 테스트케이스 화면을 좌우 분할 레이아웃으로 바꿔줘.
   - 좌측: 폴더 트리(FolderTree.vue). 프로젝트 아래 여러 폴더를 만들 수 있고, 폴더 안에 하위 폴더도 가능(중첩).
     트리 하단에 "+ 폴더 추가" 버튼.
   - 우측: 좌측에서 선택한 폴더에 속한 테스트케이스 리스트. 폴더 선택 안 하면 프로젝트 전체 테스트케이스 표시.

4. DB 스키마는 docs/05-schema-add-folders.sql 내용을 반영해줘.
   (test_case_folder 테이블 신규 + test_case에 folder_id 컬럼 추가)
   MyBatis 매퍼도 폴더 트리 조회/생성 쿼리 추가해줘 (재귀 조회는 애플리케이션 레벨에서 parent_folder_id로 조립하는 방식으로).

한번에 다 하지 말고: 1) 프로젝트 전역화(store+선택기) → 2) 폴더 트리 DB/API/화면 → 3) 나머지 화면 필터링 반영, 이 순서로 단계별로 진행하고 매 단계 확인받아줘.
```
