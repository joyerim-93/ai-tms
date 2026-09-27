package com.aitms.domain.recommend;

/**
 * 후보 생성 엔진 하나 (규칙기반 RULE / RAG / 추후 LLM).
 * 여러 엔진의 결과는 {@link CompositeRecommendationService}가 합쳐 {@link RecommendationService}로 제공.
 */
public interface RecommendationEngine {

    RecommendationResult recommend(Long requirementId);
}
