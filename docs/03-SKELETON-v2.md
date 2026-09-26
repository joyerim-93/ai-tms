# ai-tms 기본 골격 v2 (MyBatis + AI 에이전트 서비스 추가)

이전 00-SKELETON.md에서 두 가지가 바뀝니다.
1. JPA → **MyBatis**
2. AI 추천 기능을 위한 **별도 Python 마이크로서비스(ai-agent)** 추가

## 1. 전체 폴더 구조

```
ai-tms/
├── backend/                        # Spring Boot 3.x, Java 21, Gradle, MyBatis
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/aitms/
│       │   ├── AiTmsApplication.java
│       │   ├── common/
│       │   ├── config/              # MyBatis, CORS, WebClient(ai-agent 호출용) 설정
│       │   └── domain/
│       │       ├── project/
│       │       │   ├── Project.java          # VO/DTO
│       │       │   ├── ProjectMapper.java    # @Mapper 인터페이스
│       │       │   ├── ProjectService.java
│       │       │   └── ProjectController.java
│       │       ├── requirement/    # Requirement, AtomicRequirement
│       │       ├── testcase/       # TestCase, RuleCatalog
│       │       │   └── AiAgentClient.java    # ai-agent 서비스 호출 클라이언트
│       │       ├── testrun/        # TestRound, TestRoundCase
│       │       └── defect/         # Defect
│       └── resources/
│           ├── application.yml
│           ├── schema.sql
│           ├── data.sql
│           └── mapper/              # MyBatis XML 매퍼 (도메인별 1개씩)
│               ├── ProjectMapper.xml
│               ├── RequirementMapper.xml
│               ├── TestCaseMapper.xml
│               ├── TestRoundMapper.xml
│               └── DefectMapper.xml
│
├── frontend/                       # Vue 3 + Vite (변경 없음)
│   └── src/...
│
└── ai-agent/                        # ★ 신규: Python FastAPI + LangChain + LangGraph
    ├── requirements.txt             # fastapi, uvicorn, langchain, langgraph, langchain-anthropic, chromadb
    ├── app/
    │   ├── main.py                  # FastAPI 엔트리포인트
    │   ├── graph/
    │   │   ├── state.py             # LangGraph State 정의
    │   │   ├── decompose_node.py    # 요구사항 분해
    │   │   ├── rule_tool.py         # 3-1 규칙기반 도구
    │   │   ├── rag_tool.py          # 3-2 RAG 검색 도구
    │   │   ├── llm_tool.py          # 3-3 LLM 신규생성 도구
    │   │   ├── merge_node.py        # 병합/중복제거
    │   │   └── pipeline.py          # 전체 그래프 조립
    │   ├── vectorstore/
    │   │   └── chroma_store.py      # 임베디드 Chroma (로컬 파일 기반, 별도 서버 불필요)
    │   └── schemas.py               # Pydantic 요청/응답 모델
    └── data/
        └── chroma/                  # Chroma 로컬 저장 경로 (.gitignore 처리)
```

## 2. 서비스 간 통신 방식

```
Vue 프론트엔드
   │  POST /api/requirements/{id}/recommend
   ▼
Spring Boot 백엔드 (MyBatis로 H2 조회/저장)
   │  POST http://localhost:8001/recommend  (요구사항 원문 + project_id 전달)
   ▼
ai-agent (FastAPI, LangGraph 파이프라인 실행)
   │  응답: 원자요구사항 + 추천 테스트케이스 후보(rule/rag/llm 출처 태깅)
   ▼
Spring Boot가 응답 받아 H2에 draft 상태로 저장 → 프론트에 반환 → 사람 검수
```

- **DB는 H2 하나만** 씁니다. ai-agent는 H2에 직접 접속하지 않고, 필요한 데이터(원자요구사항, 참고할 기존 테스트케이스)는 Spring Boot가 REST 요청 바디에 담아 전달합니다. 결과도 ai-agent가 직접 저장하지 않고 Spring Boot에 돌려주면 Spring Boot가 저장합니다. → **AI 서비스가 죽어도 TMS 핵심 기능(1~4번)은 영향 없음**
- **RAG용 벡터 저장소**는 ai-agent 안에 Chroma를 임베디드 모드(로컬 파일, 서버 불필요)로 둡니다. 테스트케이스가 `approved` 상태로 바뀔 때 Spring Boot가 ai-agent의 `/ingest` 엔드포인트를 호출해서 벡터 저장소에 반영합니다.

## 3. MyBatis 매퍼 예시 (TestCase 기준)

```java
// TestCaseMapper.java
@Mapper
public interface TestCaseMapper {
    List<TestCase> findByProjectId(@Param("projectId") Long projectId);
    TestCase findById(@Param("id") Long id);
    void insert(TestCase testCase);
    void updateStatus(@Param("id") Long id, @Param("status") String status);
}
```

```xml
<!-- TestCaseMapper.xml -->
<mapper namespace="com.aitms.domain.testcase.TestCaseMapper">
  <select id="findByProjectId" resultType="com.aitms.domain.testcase.TestCase">
    SELECT * FROM test_case WHERE project_id = #{projectId}
  </select>
  <insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO test_case (atomic_requirement_id, project_id, title, steps, expected_result, technique, source, status, created_by)
    VALUES (#{atomicRequirementId}, #{projectId}, #{title}, #{steps}, #{expectedResult}, #{technique}, #{source}, #{status}, #{createdBy})
  </insert>
  <update id="updateStatus">
    UPDATE test_case SET status = #{status}, reviewed_at = CURRENT_TIMESTAMP WHERE id = #{id}
  </update>
</mapper>
```

## 4. ai-agent FastAPI 엔드포인트 (초안)

- `POST /decompose` — `{raw_text}` → 원자요구사항 리스트 (LLM structured output)
- `POST /recommend` — `{atomic_requirement, project_id}` → `{rule_based: [...], rag: [...], llm: [...]}`
- `POST /ingest` — `{test_case}` → 승인된 테스트케이스를 벡터 저장소에 추가
- `GET /health` — 헬스체크 (Spring Boot가 ai-agent 죽었는지 확인용)

## 5. 진행 순서 (변경)

1. **backend**: schema.sql 기준으로 MyBatis 매퍼/서비스/컨트롤러, 도메인 1개씩 (Project → Requirement → TestCase → TestRound → Defect 순)
2. **frontend**: Vue 라우터 + 4개 View + 라이트/다크 테마
3. **ai-agent 1단계**: FastAPI 골격 + `/decompose`만 먼저 (LangGraph 없이 단일 노드로 시작, 동작 확인 후 그래프화)
4. **ai-agent 2단계**: `/recommend`의 3개 도구(rule/rag/llm)를 하나씩 추가, 마지막에 LangGraph로 조립
5. **연동**: Spring Boot ↔ ai-agent REST 연동, 사람 검수 화면까지 완성

## 6. Claude Code에 넣을 프롬프트 (1단계 시작용)

세션이 이미 진행 중이면, 방향 전환을 명확히 알려주는 게 좋아요:

```
방향을 좀 바꿀게. 두 가지 변경사항 반영해줘.

1. ORM을 JPA에서 MyBatis로 바꿔줘.
   - build.gradle에서 spring-boot-starter-data-jpa 제거, mybatis-spring-boot-starter 추가
   - 도메인별로 Mapper 인터페이스(@Mapper) + resources/mapper/*.xml 로 재구성
   - 지금까지 만든 JPA 엔티티/리포지토리가 있다면 MyBatis 방식으로 다시 짜줘

2. AI 추천 기능(2번 기능 - 테스트케이스 저장소의 AI 추천)을 위해
   ai-tms/ai-agent/ 라는 별도 Python FastAPI 서비스를 새로 만들 거야.
   - FastAPI + LangChain + LangGraph 사용, requirements.txt 구성
   - 지금 단계에서는 그래프 없이 /decompose 엔드포인트 하나만 먼저 만들어줘
     (요구사항 원문을 받아서 LLM으로 원자 요구사항 리스트를 구조화해서 반환)
   - Claude API(Anthropic) 연동, API 키는 .env로 관리
   - backend의 AiAgentClient.java에서 이 /decompose를 호출하는 부분도 뼈대만 만들어줘 (실제 연결은 다음 단계)

한번에 다 하지 말고 1번(MyBatis 전환) 먼저 끝내고 확인받은 다음 2번(ai-agent) 진행해줘.
```
