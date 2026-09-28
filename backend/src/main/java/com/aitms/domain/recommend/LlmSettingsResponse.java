package com.aitms.domain.recommend;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LlmSettingsResponse {
    private boolean enabled;
    private String provider;  // 읽기 전용 표시용(anthropic|openai) — 화면에서 바꿀 수 없음

    public LlmSettingsResponse(boolean enabled, String provider) {
        this.enabled = enabled;
        this.provider = provider;
    }
}
