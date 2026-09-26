package com.aitms.recommend;

import java.util.List;

import org.springframework.stereotype.Service;

/** Claude API 연동 전 임시 구현 — 항상 빈 결과. */
@Service
public class NoopRecommendationService implements RecommendationService {

    @Override
    public List<TcRecommendation> recommend(Long requirementId, int limit) {
        return List.of();
    }
}
