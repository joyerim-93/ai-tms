# Parameterized Test Case (데이터 기반 반복 실행) 설계

## 1. 개념

TC 하나가 "시나리오(steps)"를 정의하고, 그 안의 `{변수}`에 여러 개의 값 세트(데이터셋 행)를 넣어
반복 실행합니다. 예: "가입금액 경계값 검증" TC 1개 + 데이터셋 4행(9,999원 / 10,000원 / 300만원 / 300만1원)
→ 기존처럼 TC를 4개 따로 만들 필요 없이 TC 1개로 관리.

## 2. 데이터 모델 변경 (동봉한 08-schema-add-parameterized.sql)

- `test_case.is_parameterized` — 이 TC가 데이터 기반인지 여부
- `test_case_dataset` — TC 하나에 여러 데이터 행. `param_values`는 JSON(`{"amount": 9999}`),
  필요하면 행별로 `expected_result_override`로 기대결과를 다르게 지정 가능
- `test_round_case.dataset_id` — 차수에서 실행할 때, 파라미터화 TC는 **데이터셋 행마다 결과가 개별로 남음**
  (dataset_id가 NULL이면 기존과 동일하게 TC 전체 결과)

## 3. steps 안의 플레이스홀더

`test_case.steps`, `expected_result`에 `{amount}`, `{expected}` 같은 플레이스홀더를 쓰고,
화면에서 데이터셋 행을 선택하면 해당 값으로 치환해서 보여줍니다.

```
steps: "1. 가입금액에 {amount}원 입력\n2. 가입 신청"
dataset row: {"amount": 9999}
→ 화면 표시: "1. 가입금액에 9,999원 입력\n2. 가입 신청"
```

## 4. 화면 변경

**테스트케이스 상세/등록 화면**
- "파라미터화" 토글 추가. 켜면 steps/expected_result에 `{변수명}` 입력 가능하고,
  아래에 데이터셋 테이블(행 추가/삭제, 각 열 = 변수) 편집 UI 표시

**테스트수행관리 화면**
- 파라미터화 TC를 차수에 등록하면, 데이터셋 행 개수만큼 실행 항목이 자동으로 펼쳐짐
  (예: TC 1개 등록 → 4개 실행 라인 생성)
- 각 행마다 개별로 성공/실패/block/미수행 체크
- TC 단위 진행률 표시할 때는 "4개 행 중 3개 성공, 1개 실패"처럼 하위 집계로 보여줌

## 5. AI 에이전트(규칙기반 3-1)와의 연결

규칙기반 추천 도구가 경계값 테스트를 생성할 때, 지금까지는 "TC 4개"를 만들었다면
이제는 **"파라미터화 TC 1개 + 데이터셋 4행"**을 만들도록 바뀝니다. `RuleCatalog`의 템플릿도
이 형태로 출력하도록 프롬프트/템플릿을 수정해야 합니다. (규칙기반 추천이 경계값류 요구사항에
가장 많이 쓰이므로 이 기능과 시너지가 큽니다.)

## 6. 화면 디자인은 Zephyr 실제 UI 패턴을 참고

Zephyr Scale의 실제 화면 구성 요소를 반영합니다.

**테스트케이스 목록 (Zephyr "Test Cases" 뷰)**
- 각 TC마다 사람이 읽기 쉬운 **키(Key)** 부여 (예: `TC-101`, `TC-102`) — 지금 스키마엔 없으니 `test_case.key` 컬럼 추가 필요
- 목록은 테이블형: `Key | 제목 | 폴더 | 기법(뱃지) | 출처(뱃지) | 상태(뱃지) | 데이터셋 여부`
- 파라미터화 TC는 제목 옆에 작은 "🔢 4" 같은 배지로 데이터 행 개수 표시 (Zephyr는 "Test Script" 탭에 "Data-Driven" 라벨을 붙임)

**테스트케이스 상세 (Zephyr "Test Case Detail" 뷰)**
- 상단: Key, 제목, 상태/우선순위 뱃지
- 탭 구성: `개요 | 테스트 스크립트(steps) | 데이터셋 | 실행 이력 | 연결된 요구사항`
  - "데이터셋" 탭 = Zephyr의 "Test Data" 탭과 동일한 **스프레드시트형 테이블**
    (변수명이 컬럼 헤더, 데이터 행이 각 row, 우측에 행 추가/삭제 버튼, 셀 인라인 편집)
  - "실행 이력" 탭 = 이 TC가 각 차수에서 어떤 결과였는지 시간순 리스트 (Zephyr의 "Test Execution History")
  - "연결된 요구사항" 탭 = 다대다로 연결된 요구사항 목록 + 추가/제거 (Traceability)

**테스트수행관리 (Zephyr "Test Cycle" 뷰)**
- 차수 상세 화면은 테이블형: `TC Key | 제목 | (데이터셋 행이면 하위 행으로 들여쓰기 표시) | 결과(드롭다운 뱃지) | 담당자 | 실행일시 | 코멘트`
- 결과 값은 클릭하면 바로 드롭다운으로 성공/실패/block/미수행 변경 (Zephyr는 셀 클릭 즉시 인라인 수정)
- 상단에 진행률 바 + 결과별 건수 요약 (지금 대시보드 카드와 동일한 톤 유지)

**공통 컴포넌트**
- `TestCaseKeyBadge.vue` — `TC-101` 같은 키를 모노스페이스 폰트의 옅은 배경 뱃지로 표시
- `DatasetTable.vue` — 데이터셋 탭과 테스트수행 화면 양쪽에서 재사용하는 스프레드시트형 테이블 컴포넌트
- 기존에 정했던 화이트 톤 + 상단 탭 네비 디자인(04-DESIGN-v3.md)은 그대로 유지하고, 위 화면들은 그 톤 안에서 Zephyr의 "정보 배치 방식"만 가져오는 것입니다 (색상/폰트는 우리 것 유지)

## 7. Claude Code에 넣을 프롬프트

```
docs/09-DESIGN-parameterized.md, docs/08-schema-add-parameterized.sql 읽어줘.

Zephyr의 "데이터 기반 반복 실행(Parameterized Test Case)" 기능을 추가할게.

## 스키마
- docs/08-schema-add-parameterized.sql 반영해줘.
  (test_case.is_parameterized, test_case_dataset 테이블, test_round_case.dataset_id)

## 백엔드
- TestCaseDatasetMapper 추가 (CRUD)
- TestRoundCase 등록 API에서, 등록하려는 TC가 is_parameterized=true면
  해당 TC의 데이터셋 행 전부에 대해 test_round_case를 자동 생성하도록 로직 수정
- steps/expected_result의 {변수} 치환은 프론트에서 처리 (백엔드는 원본 텍스트 + param_values JSON만 내려주면 됨)

## 프론트
- 테스트케이스 등록/수정 화면에 "파라미터화" 토글 추가.
  켜면 데이터셋 테이블 편집 UI(행 추가/삭제, JSON 파라미터를 컬럼별 입력폼으로) 표시
- 테스트케이스 상세/목록에서 파라미터화 TC는 뱃지로 구분 표시 ("데이터 4건" 같은 형태)
- 테스트수행관리 화면에서 파라미터화 TC를 차수에 등록하면 데이터셋 행 개수만큼 실행 라인이 펼쳐지고,
  각 행 steps/expected_result는 {변수}가 실제 값으로 치환되어 표시되게 해줘
- TC 단위 진행률은 "N개 행 중 M개 성공" 형태로 집계해서 보여줘

기존 규칙기반(3-1) AI 추천 도구는 이 기능이 완성된 다음에 연결할 거니까 지금은 스키마/화면만 먼저 만들어줘.
```
