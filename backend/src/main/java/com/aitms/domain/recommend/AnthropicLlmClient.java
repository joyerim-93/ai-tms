package com.aitms.domain.recommend;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;

/**
 * Claude API 호출 (Anthropic Java SDK). 클라이언트는 첫 호출 때 만들어 인증 정보가 없어도 앱은 기동됨.
 * 인증: app.ai.llm.api-key 또는 SDK 기본(ANTHROPIC_API_KEY 환경변수 / ant auth login 프로필).
 * app.ai.llm.provider=openai면 대신 {@link OpenAiLlmClient}가 뜬다(둘 중 하나만 빈으로 등록됨).
 */
@Component
@ConditionalOnProperty(prefix = "app.ai.llm", name = "provider", havingValue = "anthropic", matchIfMissing = true)
public class AnthropicLlmClient implements LlmClient {

    private static final long MAX_TOKENS = 16000L;

    private final String model;
    private final String apiKey;
    private volatile AnthropicClient client;

    public AnthropicLlmClient(@Value("${app.ai.llm.model:claude-opus-5}") String model,
                              @Value("${app.ai.llm.api-key:}") String apiKey) {
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public <T> T generate(String system, String user, Class<T> responseType) {
        StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(system)
                .outputConfig(responseType)
                .addUserMessage(user)
                .build();

        StructuredMessage<T> response = client().messages().create(params);

        if (response.stopReason().filter(r -> r.equals(StopReason.REFUSAL)).isPresent()) {
            throw new IllegalStateException("모델이 요청을 거절했습니다.");
        }
        if (response.stopReason().filter(r -> r.equals(StopReason.MAX_TOKENS)).isPresent()) {
            throw new IllegalStateException("응답이 길이 제한으로 잘렸습니다.");
        }
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("모델 응답에 텍스트가 없습니다."));
    }

    private AnthropicClient client() {
        AnthropicClient c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    var builder = AnthropicOkHttpClient.builder().timeout(Duration.ofMinutes(3));
                    client = apiKey == null || apiKey.isBlank() ? builder.fromEnv().build() : builder.apiKey(apiKey).build();
                }
                c = client;
            }
        }
        return c;
    }
}
