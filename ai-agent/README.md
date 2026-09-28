# ai-agent (Python, 신규)

AI-TMS의 나머지(backend/frontend)와 별도로 동작하는 Python 서비스의 시작점. 지금은 LLM 클라이언트 하나만 있다 —
아직 FastAPI 앱/엔드포인트는 없음(필요해지면 추가).

## LLM 클라이언트 (`app/clients/llm_client.py`)

`LLM_PROVIDER` 환경변수로 두 구현 중 하나를 고른다.

| LLM_PROVIDER | 용도 | 필요한 환경변수 |
|---|---|---|
| `openai` | 로컬 개발 — OpenAI API 직접 호출(langchain_openai) | `OPENAI_API_KEY`, `OPENAI_MODEL`(기본 `gpt-4o-mini`) |
| `internal` | 사내망 전용 — 사내 GenAI 포탈 호출 | `GENAI_BASE_URL`, `KB_KEY` |

`internal` 쪽은 실제 사내 포탈 스펙 문서 없이 "OpenAI 호환 chat/completions" 형태로 추정 작성한 것이라,
사내망에서 처음 쓸 때는 실제 응답 형식과 맞는지 확인이 필요하다(자세한 경고는 `llm_client.py` 상단 docstring).

## 준비

```bash
cd ai-agent
python3 -m venv .venv && source .venv/bin/activate   # 선택 사항이지만 권장
pip install -r requirements.txt
cp .env.example .env   # 이미 있으면 건너뛰기
```

`.env`를 열어 `OPENAI_API_KEY`에 발급받은 키를 넣는다. **`.env.example`에는 절대 실제 키를 넣지 않는다** —
그건 git에 커밋되는 템플릿이고, 실제 키는 git 제외된 `.env`에만 들어간다.

## 동작 확인

```bash
python test_llm_call.py
```

`.env`의 `LLM_PROVIDER`에 따라 openai/internal 클라이언트를 만들어 프롬프트 하나를 보내고 응답을 출력한다.
API 키 값은 어떤 경우에도 출력하지 않는다.
