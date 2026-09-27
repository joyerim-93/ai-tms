package com.aitms.domain.recommend;

/** LLM 호출 추상화 — 구조화 출력(JSON 스키마 = 응답 클래스)으로 받음. 테스트에서는 가짜 구현으로 교체. */
public interface LlmClient {

    /** @throws Exception 호출 불가(키 없음)·거절·잘림·네트워크 오류 등 — 호출 측이 경고로 바꿔 처리 */
    <T> T generate(String system, String user, Class<T> responseType) throws Exception;
}
