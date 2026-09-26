package com.aitms.domain.recommend;

import java.util.List;

/**
 * 요구사항 기반 테스트케이스 추천 (규칙 카탈로그 + RAG + LLM, 추후 LangGraph/Claude API 에이전트 연동).
 * 구현체 교체만으로 붙일 수 있도록 인터페이스만 우선 정의.
 * 흐름: 원문 요구사항 → 원자 요구사항 분해(atomic_requirement) → 타입별 규칙/RAG/LLM 추천 → DRAFT TC → 검토(승인/반려)
 */
public interface RecommendationService {

    List<TcRecommendation> recommend(Long requirementId);
}
