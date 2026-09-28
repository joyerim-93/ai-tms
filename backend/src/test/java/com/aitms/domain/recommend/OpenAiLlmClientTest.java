package com.aitms.domain.recommend;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 순수 단위 테스트(스프링 컨텍스트 없이) — 실제 OpenAI 호출은 하지 않고, 키 없음/잘못된 응답 타입 같은
 * 가드레일만 확인한다. provider=openai 조건부 빈 등록 자체는 애플리케이션 컨텍스트가 필요해 여기선 다루지 않음.
 */
class OpenAiLlmClientTest {

    @Test
    void API_키가_없으면_호출_전에_바로_실패한다() {
        OpenAiLlmClient client = new OpenAiLlmClient("gpt-4o-mini", "", new ObjectMapper());

        assertThatThrownBy(() -> client.generate("system", "user", LlmProposal.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void LlmProposal_이외의_응답_타입은_지원하지_않는다() {
        OpenAiLlmClient client = new OpenAiLlmClient("gpt-4o-mini", "sk-fake-key-for-test", new ObjectMapper());

        assertThatThrownBy(() -> client.generate("system", "user", String.class))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
