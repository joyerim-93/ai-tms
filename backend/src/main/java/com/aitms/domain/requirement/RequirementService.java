package com.aitms.domain.requirement;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.Priority;
import com.aitms.domain.recommend.RecommendationService;
import com.aitms.domain.recommend.TcRecommendation;

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
        List<AtomicRequirement> atomics = mapper.findAtomics(id);
        Map<Long, AtomicRequirement> byId = atomics.stream()
                .collect(Collectors.toMap(AtomicRequirement::getId, Function.identity()));
        mapper.findCoveringTestCases(id).forEach(tc -> byId.get(tc.getAtomicRequirementId()).getTestCases().add(tc));
        req.setAtomics(atomics);
        return req;
    }

    public List<AtomicRequirementRef> atomicRefs(Long projectId) {
        return mapper.findAtomicRefsByProject(projectId);
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
