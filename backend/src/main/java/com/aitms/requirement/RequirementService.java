package com.aitms.requirement;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.Priority;
import com.aitms.recommend.RecommendationService;
import com.aitms.recommend.TcRecommendation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequirementService {

    private final RequirementMapper mapper;
    private final RecommendationService recommendationService;

    public List<Requirement> findByProject(Long projectId) {
        return mapper.findByProject(projectId);
    }

    public Requirement get(Long id) {
        Requirement req = mapper.findById(id)
                .orElseThrow(() -> ApiException.notFound("요구사항을 찾을 수 없습니다. id=" + id));
        req.setAtomics(mapper.findAtomics(id));
        return req;
    }

    @Transactional
    public Requirement create(RequirementRequest req) {
        Requirement r = new Requirement();
        r.setProjectId(req.projectId());
        r.setTitle(req.title().strip());
        r.setDescription(req.description().strip());
        r.setSource(RequirementSource.MANUAL);
        r.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        mapper.insert(r);
        return get(r.getId());
    }

    /** AI 추천 트리거 — 현재는 NoopRecommendationService (빈 결과) */
    public List<TcRecommendation> recommend(Long id) {
        get(id);
        return recommendationService.recommend(id);
    }
}
