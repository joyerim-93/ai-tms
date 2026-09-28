package com.aitms.domain.recommend;

import jakarta.validation.constraints.NotNull;

/** 화면의 LLM 추천 on/off 토글 요청. */
public record LlmSettingsRequest(@NotNull Boolean enabled) {
}
