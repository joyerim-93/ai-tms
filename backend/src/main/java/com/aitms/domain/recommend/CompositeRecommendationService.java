package com.aitms.domain.recommend;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** 등록된 모든 추천 엔진(RULE, RAG, …)의 후보·경고를 순서대로 합침. 엔진을 추가하려면 RecommendationEngine 빈만 만들면 됨. */
@Service
@Primary
@RequiredArgsConstructor
public class CompositeRecommendationService implements RecommendationService {

    private final List<RecommendationEngine> engines;

    @Override
    public RecommendationResult recommend(Long requirementId) {
        List<TcRecommendation> candidates = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (RecommendationEngine engine : engines) {
            RecommendationResult r = engine.recommend(requirementId);
            candidates.addAll(r.candidates());
            warnings.addAll(r.warnings());
        }
        return new RecommendationResult(candidates, warnings);
    }
}
