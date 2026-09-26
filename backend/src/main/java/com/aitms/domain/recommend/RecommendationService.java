package com.aitms.domain.recommend;

/**
 * 요구사항 기반 테스트케이스 추천.
 * 흐름: 원문 요구사항 → 원자 요구사항(atomic_requirement) → 타입별 규칙(RULE) / RAG / LLM 후보 → DRAFT TC → 검토(승인/반려)
 * 현재 구현: 규칙기반(RuleBasedRecommendationService). RAG·LLM은 ai-agent 연동 시 구현체 추가/합성.
 * 후보 생성만 담당하고 저장은 RequirementService 가 함.
 */
public interface RecommendationService {

    RecommendationResult recommend(Long requirementId);
}
