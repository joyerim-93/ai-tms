package com.aitms.recommend;

import java.math.BigDecimal;

/** score: 0~1 유사도, reason: 추천 근거(LLM 생성). */
public record TcRecommendation(Long testCaseId, BigDecimal score, String reason) {
}
