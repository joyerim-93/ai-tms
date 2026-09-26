package com.aitms.domain.recommend;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.aitms.domain.requirement.AtomicRequirement;
import com.aitms.domain.requirement.RequirementMapper;
import com.aitms.domain.testcase.TcSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 규칙기반 추천 (3-1) — rule_catalog.generator 로 '파라미터화 TC 1개 + 데이터셋 N행' 후보 생성.
 * <ul>
 *   <li>[[text]] [[unit]] [[flag]] [[bonus]]: 생성 시점에 원자 요구사항 값으로 치환</li>
 *   <li>{value} {option} {flag} {expected}: TC 단계에 남는 데이터셋 변수 (실행 화면에서 치환)</li>
 *   <li>rows[].value: min, max, min-1, min+1, max-1, max+1 (±는 generator.step 단위)</li>
 *   <li>rowsFrom=options: conditions.options 선택지마다 1행, conditions 의 *_map 값을 {mapped}로</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RuleBasedRecommendationService implements RecommendationService {

    private static final BigDecimal SCORE = BigDecimal.ONE;

    private final RequirementMapper requirementMapper;
    private final RuleCatalogMapper ruleCatalogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public RecommendationResult recommend(Long requirementId) {
        List<TcRecommendation> candidates = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (AtomicRequirement atomic : requirementMapper.findAtomics(requirementId)) {
            List<RuleCatalog> rules = ruleCatalogMapper.findByType(atomic.getType()).stream()
                    .filter(r -> r.getGenerator() != null)
                    .toList();
            if (rules.isEmpty()) {
                warnings.add(label(atomic) + ": 적용할 규칙이 없습니다.");
            }
            for (RuleCatalog rule : rules) {
                try {
                    generate(atomic, rule).ifPresentOrElse(candidates::add,
                            () -> warnings.add(label(atomic) + ": 규칙에 필요한 값(범위/조건)이 없습니다."));
                } catch (Exception e) {
                    warnings.add(label(atomic) + ": 생성 규칙 해석 실패 — " + e.getMessage());
                }
            }
        }
        return new RecommendationResult(candidates, warnings);
    }

    private Optional<TcRecommendation> generate(AtomicRequirement atomic, RuleCatalog rule) throws Exception {
        JsonNode gen = objectMapper.readTree(rule.getGenerator());
        JsonNode conditions = atomic.getConditions() == null ? objectMapper.createObjectNode()
                : objectMapper.readTree(atomic.getConditions());

        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("text", atomic.getAtomicText());
        ctx.put("unit", unitLabel(atomic.getUnit()));
        ctx.put("flag", conditions.path("flag").asText("flag"));
        ctx.put("bonus", bonusText(conditions));

        List<TcRecommendation.DatasetRow> rows = gen.has("rowsFrom")
                ? optionRows(gen, conditions)
                : fixedRows(gen, atomic, ctx);
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        List<TcRecommendation.Step> steps = new ArrayList<>();
        for (JsonNode s : gen.path("steps")) {
            steps.add(new TcRecommendation.Step(fill(s.path("action").asText(), ctx),
                    s.path("expected").isNull() ? null : fill(s.path("expected").asText(), ctx)));
        }
        return Optional.of(new TcRecommendation(atomic.getId(), fill(gen.path("title").asText(), ctx),
                rule.getTechnique(), TcSource.RULE, true, steps, rows, null, SCORE));
    }

    /** rows[] 고정 행 — 경계값(value 식) 또는 플래그(flag) */
    private List<TcRecommendation.DatasetRow> fixedRows(JsonNode gen, AtomicRequirement atomic, Map<String, String> ctx) {
        BigDecimal step = gen.has("step") ? gen.path("step").decimalValue() : BigDecimal.ONE;
        String unit = unitLabel(atomic.getUnit());
        List<TcRecommendation.DatasetRow> rows = new ArrayList<>();
        for (JsonNode r : gen.path("rows")) {
            Map<String, Object> params = new LinkedHashMap<>();
            String label = r.path("label").asText();
            if (r.has("value")) {
                BigDecimal v = evaluate(r.path("value").asText(), atomic.getMinValue(), atomic.getMaxValue(), step);
                if (v == null) {
                    return List.of(); // 범위 값 없음
                }
                params.put("value", toJsonNumber(v));
                label = label + " (" + format(v) + unit + ")";
            }
            if (r.has("flag")) {
                params.put("flag", r.path("flag").asBoolean());
            }
            String expected = r.path("expected").isMissingNode() ? null : fill(r.path("expected").asText(), ctx);
            rows.add(new TcRecommendation.DatasetRow(label, params, expected));
        }
        return rows;
    }

    /** rowsFrom=options — 조건 선택지마다 1행, *_map 매핑 값은 {mapped} */
    private List<TcRecommendation.DatasetRow> optionRows(JsonNode gen, JsonNode conditions) {
        JsonNode map = null;
        for (Iterator<String> it = conditions.fieldNames(); it.hasNext(); ) {
            String key = it.next();
            if (key.endsWith("_map")) {
                map = conditions.get(key);
                break;
            }
        }
        List<TcRecommendation.DatasetRow> rows = new ArrayList<>();
        for (JsonNode opt : conditions.path("options")) {
            String option = opt.asText();
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("option", option);
            String mapped = "-";
            if (map != null && map.has(option)) {
                JsonNode m = map.get(option);
                params.put("mapped", m.isNumber() ? toJsonNumber(m.decimalValue()) : m.asText());
                mapped = m.isNumber() ? format(m.decimalValue()) : m.asText();
            }
            String label = gen.path("label").asText("{option}").replace("{option}", option);
            String expected = gen.path("expected").asText("").replace("{option}", option).replace("{mapped}", mapped);
            rows.add(new TcRecommendation.DatasetRow(label, params, expected));
        }
        return rows;
    }

    static BigDecimal evaluate(String expr, BigDecimal min, BigDecimal max, BigDecimal step) {
        String e = expr.replace(" ", "");
        BigDecimal base = e.startsWith("min") ? min : e.startsWith("max") ? max : null;
        if (base == null) {
            return null;
        }
        if (e.length() == 3) {
            return base;
        }
        BigDecimal times = new BigDecimal(e.substring(4));
        BigDecimal delta = step.multiply(times);
        return e.charAt(3) == '+' ? base.add(delta) : base.subtract(delta);
    }

    /** 정수면 Long, 아니면 BigDecimal (JSON 숫자로 저장) */
    static Object toJsonNumber(BigDecimal v) {
        BigDecimal s = v.stripTrailingZeros();
        return s.scale() <= 0 ? (Object) s.longValueExact() : s;
    }

    static String format(BigDecimal v) {
        return new DecimalFormat("#,##0.####").format(v);
    }

    static String unitLabel(String unit) {
        if (unit == null) {
            return "";
        }
        return switch (unit) {
            case "KRW" -> "원";
            case "percent" -> "%";
            case "year" -> "년";
            default -> unit;
        };
    }

    /** conditions 의 *bonus* 수치 → " (+0.2)" */
    private static String bonusText(JsonNode conditions) {
        for (Iterator<String> it = conditions.fieldNames(); it.hasNext(); ) {
            String key = it.next();
            if (key.contains("bonus") && conditions.get(key).isNumber()) {
                return " (+" + format(conditions.get(key).decimalValue()) + ")";
            }
        }
        return "";
    }

    private static String fill(String template, Map<String, String> ctx) {
        String out = template;
        for (Map.Entry<String, String> e : ctx.entrySet()) {
            out = out.replace("[[" + e.getKey() + "]]", e.getValue());
        }
        return out;
    }

    private static String label(AtomicRequirement a) {
        return "원자 요구사항 #" + a.getId() + "(" + a.getAtomicText() + ")";
    }
}
