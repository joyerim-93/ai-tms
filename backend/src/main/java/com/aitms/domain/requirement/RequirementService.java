package com.aitms.domain.requirement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.Priority;
import com.aitms.domain.recommend.RecommendationService;
import com.aitms.domain.recommend.RecommendationResult;
import com.aitms.domain.recommend.TcRecommendation;
import com.aitms.domain.testcase.TestCase;
import com.aitms.domain.testcase.TestCaseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequirementService {

    private final RequirementMapper mapper;
    private final RecommendationService recommendationService;
    private final TestCaseService testCaseService;

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

    /**
     * 추천 실행 — 후보를 DRAFT TC로 저장(같은 추천 TC가 있으면 건너뜀). 사람이 TC 상세에서 승인/반려.
     * 현재 후보 엔진: 규칙기반(RuleBasedRecommendationService)
     */
    @Transactional
    public RecommendResponse recommend(Long id) {
        Requirement req = get(id);
        RecommendationResult result = recommendationService.recommend(id);
        List<TestCase> created = new ArrayList<>();
        int skipped = 0;
        for (TcRecommendation rec : result.candidates()) {
            Optional<TestCase> tc = testCaseService.createDraft(rec, req.getProjectId());
            if (tc.isPresent()) {
                created.add(tc.get());
            } else {
                skipped++;
            }
        }
        return new RecommendResponse(created, skipped, result.warnings());
    }
}
