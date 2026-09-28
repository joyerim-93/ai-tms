package com.aitms.domain.recommend;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** 요구사항 탭의 'AI 생성(LLM) 추천' 토글 — 로그인만 하면 누구나 껐다 켤 수 있음(RBAC 미도입, 전역 컨벤션과 동일). */
@RestController
@RequestMapping("/api/llm-settings")
@RequiredArgsConstructor
public class LlmSettingsController {

    private final LlmSettings settings;

    @GetMapping
    public LlmSettingsResponse get() {
        return new LlmSettingsResponse(settings.isEnabled(), settings.getProvider());
    }

    @PatchMapping
    public LlmSettingsResponse update(@Validated @RequestBody LlmSettingsRequest req) {
        settings.setEnabled(req.enabled());
        return get();
    }
}
