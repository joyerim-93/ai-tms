package com.aitms.domain.requirement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aitms.common.ApiException;
import com.aitms.common.Priority;
import com.aitms.domain.recommend.LlmClient;
import com.aitms.domain.recommend.LlmSettings;
import com.aitms.domain.recommend.RecommendationService;
import com.aitms.domain.recommend.RecommendationResult;
import com.aitms.domain.recommend.TcRecommendation;
import com.aitms.domain.testcase.TcSource;
import com.aitms.domain.testcase.TestCase;
import com.aitms.domain.testcase.TestCaseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequirementService {

    private static final Logger log = LoggerFactory.getLogger(RequirementService.class);

    static final String DECOMPOSE_SYSTEM = """
            당신은 금융 서비스 요구사항을 분석하는 시니어 QA 엔지니어입니다.
            요구사항 원문을 읽고, 테스트 가능한 단위(원자 요구사항)로 쪼갭니다.

            지침:
            - 원문에 실제로 적힌 조건만 뽑고, 없는 수치나 정책을 지어내지 않습니다.
            - 금액/비율처럼 범위가 있는 조건은 AMOUNT_RANGE/RATE_RANGE로, 기간·기한 조건은 PERIOD_CONDITION으로,
              참/거짓으로 판단하는 자격·조건은 BOOLEAN_FLAG로 분류합니다.
            - 하나의 원자 요구사항은 하나의 검증 대상만 담습니다(여러 조건을 한 문장에 욱여넣지 않음).
            - 모든 텍스트는 한국어로 씁니다.
            - 분해할 만한 조건이 없으면 빈 목록을 돌려줍니다.
            """;

    private final RequirementMapper mapper;
    private final RecommendationService recommendationService;
    private final TestCaseService testCaseService;
    private final LlmClient llm;
    private final LlmSettings llmSettings;

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
        Map<Long, java.math.BigDecimal> scores = new java.util.LinkedHashMap<>();
        for (TcRecommendation rec : result.candidates()) {
            Optional<TestCase> tc = testCaseService.createDraft(rec, req.getProjectId());
            if (tc.isPresent()) {
                created.add(tc.get());
                if (rec.source() == TcSource.RAG) {
                    scores.put(tc.get().getId(), rec.score());
                }
            } else {
                skipped++;
            }
        }
        return new RecommendResponse(created, skipped, result.warnings(), scores);
    }

    /**
     * 요구사항 원문을 LLM으로 원자 요구사항으로 분해해서 저장 — 원자 요구사항이 있어야 규칙기반/RAG/LLM 추천이 동작하는데,
     * 지금까지는 REQ-001처럼 미리 심어둔 샘플뿐이라 사람이 직접 등록한 요구사항은 추천을 받을 방법이 없었음.
     * 이미 원자 요구사항이 있으면 LLM을 다시 부르지 않고 그대로 반환(중복 생성·비용 낭비 방지).
     */
    @Transactional
    public Requirement decompose(Long id) {
        Requirement req = get(id);
        if (!req.getAtomics().isEmpty()) {
            return req;
        }
        if (!llmSettings.isEnabled()) {
            throw ApiException.conflict("LLM 추천이 꺼져 있어 원자 요구사항을 분해할 수 없습니다. 요구사항 탭 상단의 토글을 켜주세요.");
        }
        AtomicDecomposition decomposition;
        try {
            decomposition = llm.generate(DECOMPOSE_SYSTEM, req.getDescription(), AtomicDecomposition.class);
        } catch (Exception e) {
            log.warn("원자 요구사항 분해 실패 (requirement {})", id, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "원자 요구사항 분해에 실패했습니다: " + reason(e));
        }
        List<AtomicDecomposition.Atomic> atomics = decomposition == null || decomposition.atomics() == null
                ? List.of() : decomposition.atomics();
        for (AtomicDecomposition.Atomic a : atomics) {
            if (a == null || a.atomicText() == null || a.atomicText().isBlank() || a.type() == null) {
                continue; // 형식이 이상한 제안은 조용히 건너뜀
            }
            AtomicRequirement entity = new AtomicRequirement();
            entity.setRequirementId(id);
            entity.setAtomicText(a.atomicText().strip());
            entity.setType(a.type());
            entity.setMinValue(a.minValue());
            entity.setMaxValue(a.maxValue());
            entity.setUnit(a.unit());
            entity.setConditions(a.conditions());
            mapper.insertAtomic(entity);
        }
        return get(id);
    }

    private static String reason(Exception e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return m.length() > 200 ? m.substring(0, 200) + "…" : m;
    }
}
