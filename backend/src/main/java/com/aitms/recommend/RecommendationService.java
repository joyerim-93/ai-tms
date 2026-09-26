package com.aitms.recommend;

import java.util.List;

/**
 * 요구사항 기반 테스트케이스 추천 (RAG + Claude API 예정).
 * 구현체 교체만으로 붙일 수 있도록 인터페이스만 우선 정의. 결과는 requirement_tc(source=AI)로 저장.
 */
public interface RecommendationService {

    List<TcRecommendation> recommend(Long requirementId, int limit);
}
