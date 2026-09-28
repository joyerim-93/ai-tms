"""LLM 클라이언트 동작 확인용 스크립트.

.env의 설정(LLM_PROVIDER 등)으로 get_llm_client()가 만든 클라이언트에 간단한 프롬프트 하나를
보내고 응답을 출력한다. API 키 값은 어떤 경우에도 출력하지 않는다.

실행: python test_llm_call.py  (ai-agent/ 디렉터리에서, requirements.txt 설치 후)
"""
from __future__ import annotations

import os

from dotenv import load_dotenv

load_dotenv()

from app.clients.llm_client import get_llm_client  # noqa: E402  (load_dotenv 이후 import)


def main() -> None:
    provider = os.environ.get("LLM_PROVIDER", "internal")
    print(f"[test_llm_call] LLM_PROVIDER={provider}")

    client = get_llm_client()
    prompt = "한 문장으로 자기소개를 해줘."
    print(f"[test_llm_call] 프롬프트: {prompt}")

    response = client.complete(prompt)
    print("[test_llm_call] 응답:")
    print(response)


if __name__ == "__main__":
    main()
