package com.aitms.domain.recommend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.aitms.common.ApiException;
import com.aitms.domain.requirement.AtomicRequirement;
import com.aitms.domain.requirement.Requirement;
import com.aitms.domain.requirement.RequirementMapper;
import com.aitms.domain.testcase.TcSource;
import com.aitms.domain.testcase.TestCase;
import com.aitms.domain.testcase.TestCaseDataset;
import com.aitms.domain.testcase.TestCaseDatasetMapper;
import com.aitms.domain.testcase.TestCaseMapper;
import com.aitms.domain.testcase.TestStep;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * RAG 추천 (3-2) — 원자 요구사항마다 다른 프로젝트의 승인된 유사 TC를 찾아 후보로 복제(source=RAG, origin_project_id=원본 프로젝트).
 * 검색 범위: 현재 프로젝트 제외, 전체 프로젝트의 APPROVED·ACTIVE TC. 검색기({@link TcRetriever})는 교체 가능.
 * 단계·데이터셋은 원본 그대로 복사, 요구사항 연결은 현재 원자 요구사항으로 새로 걸림(저장 시).
 */
@Service
@RequiredArgsConstructor
public class RagRecommendationService implements RecommendationEngine {

    static final int TOP_N = 3;

    private final RequirementMapper requirementMapper;
    private final TestCaseMapper testCaseMapper;
    private final TestCaseDatasetMapper datasetMapper;
    private final TcRetriever retriever;
    private final ObjectMapper objectMapper;

    @Override
    public RecommendationResult recommend(Long requirementId) {
        Requirement req = requirementMapper.findById(requirementId)
                .orElseThrow(() -> ApiException.notFound("요구사항을 찾을 수 없습니다: " + requirementId));
        // 같은 원본 TC가 여러 원자 요구사항에 걸리면 가장 유사한 원자 요구사항 하나에만 복제
        Map<Long, Match> best = new LinkedHashMap<>();
        for (AtomicRequirement atomic : requirementMapper.findAtomics(requirementId)) {
            for (TcRetriever.Hit hit : retriever.search(atomic, req.getProjectId(), TOP_N)) {
                Match cur = best.get(hit.testCaseId());
                if (cur == null || hit.score() > cur.hit().score()) {
                    best.put(hit.testCaseId(), new Match(atomic, hit));
                }
            }
        }
        List<TcRecommendation> candidates = new ArrayList<>();
        best.values().forEach(m -> candidates.add(toRecommendation(m.atomic(), m.hit())));
        return new RecommendationResult(candidates, new ArrayList<>());
    }

    private record Match(AtomicRequirement atomic, TcRetriever.Hit hit) {
    }

    private TcRecommendation toRecommendation(AtomicRequirement atomic, TcRetriever.Hit hit) {
        TestCase src = testCaseMapper.findById(hit.testCaseId()).orElseThrow();
        List<TcRecommendation.Step> steps = testCaseMapper.findSteps(src.getId()).stream()
                .sorted(java.util.Comparator.comparing(TestStep::getStepNo))
                .map(s -> new TcRecommendation.Step(s.getAction(), s.getExpectedResult()))
                .toList();
        boolean parameterized = Boolean.TRUE.equals(src.getIsParameterized());
        List<TcRecommendation.DatasetRow> rows = parameterized ? datasetRows(src.getId()) : List.of();
        return new TcRecommendation(atomic.getId(), src.getTitle(), src.getTechnique(), TcSource.RAG,
                parameterized && !rows.isEmpty(), steps, rows, src.getProjectId(),
                BigDecimal.valueOf(hit.score()).setScale(2, RoundingMode.HALF_UP));
    }

    private List<TcRecommendation.DatasetRow> datasetRows(Long testCaseId) {
        List<TcRecommendation.DatasetRow> rows = new ArrayList<>();
        for (TestCaseDataset d : datasetMapper.findByTestCase(testCaseId)) {
            rows.add(new TcRecommendation.DatasetRow(d.getRowLabel(), parse(d.getParamValues()), d.getExpectedResultOverride()));
        }
        return rows;
    }

    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("데이터셋 JSON 해석 실패: " + json, e);
        }
    }
}
