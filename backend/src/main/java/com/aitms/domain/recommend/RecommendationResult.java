package com.aitms.domain.recommend;

import java.util.List;

/**
 * @param candidates 생성된 TC 후보
 * @param warnings   후보를 만들지 못한 원자 요구사항 사유 (예: 범위 값 없음, 규칙 없음)
 */
public record RecommendationResult(List<TcRecommendation> candidates, List<String> warnings) {
}
