package com.aitms.recommend;

import java.util.List;

import org.springframework.stereotype.Service;

/** AI 에이전트 연동 전 임시 구현 — 항상 빈 결과. */
@Service
public class NoopRecommendationService implements RecommendationService {

    @Override
    public List<TcRecommendation> recommend(Long requirementId) {
        return List.of();
    }
}
