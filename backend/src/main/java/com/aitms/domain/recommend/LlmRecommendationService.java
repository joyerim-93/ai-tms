package com.aitms.domain.recommend;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aitms.common.ApiException;
import com.aitms.domain.requirement.AtomicRequirement;
import com.aitms.domain.requirement.CoveringTestCase;
import com.aitms.domain.requirement.Requirement;
import com.aitms.domain.requirement.RequirementMapper;
import com.aitms.domain.testcase.TcSource;
import com.aitms.domain.testcase.TestTechnique;

/**
 * LLM 추천 (3-3) — Claude 가 요구사항 원문·원자 요구사항·이미 있는 TC를 보고, 규칙/RAG 가 놓치기 쉬운
 * 교차 조합·예외 흐름·정합성 관점의 TC를 새로 제안 (source=LLM, DRAFT, 검토 후 승인).
 * 요구사항 1건당 API 1회 호출. 호출 실패·미설정은 경고로 남기고 다른 엔진 결과는 그대로 유지.
 */
@Service
public class LlmRecommendationService implements RecommendationEngine {

    private static final Logger log = LoggerFactory.getLogger(LlmRecommendationService.class);

    static final String SYSTEM = """
            당신은 금융 서비스를 다루는 시니어 QA 엔지니어입니다.
            요구사항 원문과 원자 요구사항, 그리고 이미 등록된 테스트케이스를 보고 아직 검증되지 않은 부분을 채우는 테스트케이스를 제안합니다.

            지침:
            - 이미 있는 테스트케이스와 겹치지 않게 하고, 경계값·동등분할 같은 기본 케이스보다 교차 조합, 예외/오류 흐름, 상태·순서, 계산 정합성처럼 놓치기 쉬운 관점을 우선합니다.
            - 요구사항 문장에서 근거를 찾을 수 있는 것만 제안하고, 문서에 없는 정책이나 수치를 지어내지 않습니다.
            - 각 테스트케이스는 가장 관련 있는 원자 요구사항 하나에 연결합니다(id는 주어진 목록에서만 선택).
            - 단계는 사람이 그대로 따라 할 수 있게 구체적으로 쓰고, 마지막 단계에는 반드시 기대 결과를 씁니다.
            - 모든 텍스트는 한국어로 씁니다.
            - 추가할 만한 것이 없으면 빈 목록을 돌려줍니다. 개수를 채우려고 억지로 만들지 마세요.
            """;

    private final RequirementMapper requirementMapper;
    private final LlmClient llm;
    private final boolean enabled;
    private final int maxCases;

    public LlmRecommendationService(RequirementMapper requirementMapper, LlmClient llm,
                                    @Value("${app.ai.llm.enabled:true}") boolean enabled,
                                    @Value("${app.ai.llm.max-cases:5}") int maxCases) {
        this.requirementMapper = requirementMapper;
        this.llm = llm;
        this.enabled = enabled;
        this.maxCases = maxCases;
    }

    @Override
    public RecommendationResult recommend(Long requirementId) {
        if (!enabled) {
            return new RecommendationResult(List.of(), List.of());
        }
        Requirement req = requirementMapper.findById(requirementId)
                .orElseThrow(() -> ApiException.notFound("요구사항을 찾을 수 없습니다: " + requirementId));
        List<AtomicRequirement> atomics = requirementMapper.findAtomics(requirementId);
        if (atomics.isEmpty()) {
            return new RecommendationResult(List.of(), List.of("AI 생성 추천: 원자 요구사항이 없어 건너뛰었습니다."));
        }
        List<CoveringTestCase> existing = requirementMapper.findCoveringTestCases(requirementId);

        LlmProposal proposal;
        try {
            proposal = llm.generate(SYSTEM, buildPrompt(req, atomics, existing), LlmProposal.class);
        } catch (Exception e) {
            log.warn("LLM 추천 실패 (requirement {})", requirementId, e);
            return new RecommendationResult(List.of(), List.of("AI 생성 추천을 건너뛰었습니다: " + reason(e)));
        }
        return toResult(proposal, atomics);
    }

    String buildPrompt(Requirement req, List<AtomicRequirement> atomics, List<CoveringTestCase> existing) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 요구사항 ").append(req.getReqCode()).append(" — ").append(req.getTitle()).append("\n")
                .append(req.getDescription()).append("\n\n");

        sb.append("## 원자 요구사항 (연결할 id 목록)\n");
        for (AtomicRequirement a : atomics) {
            sb.append("- id=").append(a.getId()).append(" [").append(a.getType()).append("] ").append(a.getAtomicText());
            if (a.getMinValue() != null || a.getMaxValue() != null) {
                sb.append(" (범위 ").append(a.getMinValue() != null ? a.getMinValue().toPlainString() : "").append(" ~ ")
                        .append(a.getMaxValue() != null ? a.getMaxValue().toPlainString() : "")
                        .append(a.getUnit() != null ? " " + a.getUnit() : "").append(")");
            }
            if (a.getConditions() != null) {
                sb.append(" 조건=").append(a.getConditions());
            }
            sb.append("\n");
        }

        sb.append("\n## 이미 등록된 테스트케이스 (겹치지 않게)\n");
        if (existing.isEmpty()) {
            sb.append("(없음)\n");
        }
        for (CoveringTestCase t : existing) {
            sb.append("- 원자 id=").append(t.getAtomicRequirementId()).append(": ").append(t.getTitle()).append("\n");
        }
        sb.append("\n최대 ").append(maxCases).append("건까지 제안해 주세요.");
        return sb.toString();
    }

    RecommendationResult toResult(LlmProposal proposal, List<AtomicRequirement> atomics) {
        Set<Long> validIds = atomics.stream().map(AtomicRequirement::getId).collect(Collectors.toSet());
        List<TcRecommendation> candidates = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int dropped = 0;
        List<LlmProposal.Case> cases = proposal == null || proposal.cases() == null ? List.of() : proposal.cases();
        for (LlmProposal.Case c : cases) {
            if (candidates.size() >= maxCases) {
                break;
            }
            TcRecommendation rec = convert(c, validIds);
            if (rec == null) {
                dropped++;
            } else {
                candidates.add(rec);
            }
        }
        if (dropped > 0) {
            warnings.add("AI 생성 추천: 형식이 올바르지 않은 제안 " + dropped + "건은 제외했습니다.");
        }
        return new RecommendationResult(candidates, warnings);
    }

    private static TcRecommendation convert(LlmProposal.Case c, Set<Long> validIds) {
        if (c == null || !validIds.contains(c.atomicRequirementId()) || c.title() == null || c.title().isBlank()
                || c.title().strip().length() > 300 || c.steps() == null || c.steps().isEmpty()) {
            return null;
        }
        List<TcRecommendation.Step> steps = new ArrayList<>();
        for (LlmProposal.Step s : c.steps()) {
            if (s == null || s.action() == null || s.action().isBlank() || s.action().length() > 2000
                    || (s.expectedResult() != null && s.expectedResult().length() > 2000)) {
                return null;
            }
            String expected = s.expectedResult() == null || s.expectedResult().isBlank() ? null : s.expectedResult().strip();
            steps.add(new TcRecommendation.Step(s.action().strip(), expected));
        }
        TestTechnique technique = c.technique() != null ? c.technique() : TestTechnique.EXPLORATORY;
        return new TcRecommendation(c.atomicRequirementId(), c.title().strip(), technique, TcSource.LLM,
                false, steps, List.of(), null, (BigDecimal) null);
    }

    private static String reason(Exception e) {
        String type = e.getClass().getSimpleName();
        if (type.equals("UnauthorizedException") || type.equals("PermissionDeniedException")) {
            return "Claude API 인증 정보가 없거나 올바르지 않습니다 (ANTHROPIC_API_KEY 설정 확인)";
        }
        String m = e.getMessage();
        if (m == null || m.isBlank()) {
            return type;
        }
        return m.length() > 200 ? m.substring(0, 200) + "…" : m;
    }
}
