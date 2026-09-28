package com.aitms.domain.recommend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * LLM(3-3) 추천 on/off를 **런타임에** 바꿀 수 있게 해주는 상태 보관소.
 * 초기값은 application.yml의 app.ai.llm.enabled(기본 false, 비용 때문에 꺼둠) — 화면의 토글로
 * 껐다 켰다 할 수 있지만, 메모리에만 있어서 **서버 재기동하면 다시 초기값(설정 파일 기준)으로 돌아간다**
 * (계속 켜진 채로 잊어버리는 걸 막기 위한 의도적인 동작).
 * provider(anthropic/openai)는 빈 선택(@ConditionalOnProperty) 시점에 고정되는 값이라 런타임에는 못 바꿈 — 읽기 전용으로만 노출.
 */
@Component
public class LlmSettings {

    private final String provider;
    private volatile boolean enabled;

    public LlmSettings(@Value("${app.ai.llm.enabled:false}") boolean enabled,
                       @Value("${app.ai.llm.provider:anthropic}") String provider) {
        this.enabled = enabled;
        this.provider = provider;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }
}
