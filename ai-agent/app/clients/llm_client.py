"""LLM 클라이언트 — LLM_PROVIDER 환경변수로 openai / internal 두 구현을 분기한다.

- LLM_PROVIDER=openai   : 로컬 개발용. langchain_openai.ChatOpenAI로 OpenAI API를 직접 호출.
                          OPENAI_API_KEY(필수) / OPENAI_MODEL(기본 gpt-4o-mini)로 설정.
- LLM_PROVIDER=internal : 사내망 전용. 사내 GenAI 포탈(GENAI_BASE_URL + KB_KEY)을 HTTP로 호출.

⚠ 사내 GenAI 포탈용 구현(InternalGenAiLlmClient)은 이 프로젝트에 실제 포탈 연동 코드가
이전에 없었기 때문에, 스펙 문서 없이 "OpenAI 호환 chat/completions" 형태로 추정 작성한 것이다.
사내망에서 처음 사용할 때는 실제 요청/응답 필드·인증 헤더 이름이 맞는지 반드시 확인·수정할 것.

API 키(OPENAI_API_KEY, KB_KEY)는 절대 로그·예외 메시지·print에 값 그대로 남기지 않는다.
"""
from __future__ import annotations

import os
from abc import ABC, abstractmethod


class LlmClient(ABC):
    """모든 LLM 클라이언트 구현이 따르는 공통 인터페이스."""

    @abstractmethod
    def complete(self, prompt: str) -> str:
        """단일 프롬프트를 보내고 텍스트 응답을 반환한다."""
        raise NotImplementedError


class OpenAiLlmClient(LlmClient):
    """로컬 개발용 — OpenAI API를 langchain_openai로 직접 호출."""

    def __init__(self) -> None:
        try:
            from langchain_openai import ChatOpenAI
        except ImportError as exc:  # pragma: no cover
            raise RuntimeError(
                "langchain-openai가 설치되어 있지 않습니다. `pip install -r requirements.txt`를 실행하세요."
            ) from exc

        api_key = os.environ.get("OPENAI_API_KEY")
        if not api_key:
            raise RuntimeError("OPENAI_API_KEY 환경변수가 설정되지 않았습니다. (.env 확인)")
        model = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")
        self._model = ChatOpenAI(model=model, api_key=api_key)

    def complete(self, prompt: str) -> str:
        result = self._model.invoke(prompt)
        return result.content


class InternalGenAiLlmClient(LlmClient):
    """사내망 전용 — 사내 GenAI 포탈 호출 (스펙 추정 구현, 위 모듈 docstring 경고 참고)."""

    def __init__(self) -> None:
        try:
            import requests
        except ImportError as exc:  # pragma: no cover
            raise RuntimeError(
                "requests가 설치되어 있지 않습니다. `pip install -r requirements.txt`를 실행하세요."
            ) from exc

        self._requests = requests
        base_url = os.environ.get("GENAI_BASE_URL")
        api_key = os.environ.get("KB_KEY")
        if not base_url or not api_key:
            raise RuntimeError("GENAI_BASE_URL / KB_KEY 환경변수가 설정되지 않았습니다. (.env 확인)")
        self._base_url = base_url.rstrip("/")
        self._api_key = api_key

    def complete(self, prompt: str) -> str:
        response = self._requests.post(
            f"{self._base_url}/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {self._api_key}",
                "Content-Type": "application/json",
            },
            json={"messages": [{"role": "user", "content": prompt}]},
            timeout=30,
        )
        response.raise_for_status()
        data = response.json()
        return data["choices"][0]["message"]["content"]


def get_llm_client() -> LlmClient:
    """LLM_PROVIDER 환경변수(기본값 internal)로 알맞은 클라이언트를 생성해 반환한다."""
    provider = os.environ.get("LLM_PROVIDER", "internal").strip().lower()
    if provider == "openai":
        return OpenAiLlmClient()
    if provider == "internal":
        return InternalGenAiLlmClient()
    raise ValueError(f"지원하지 않는 LLM_PROVIDER 값입니다: '{provider}' (openai | internal 중 하나)")
