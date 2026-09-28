package com.aitms.domain.requirement;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** LLM이 요구사항 원문을 분해한 결과 — 이 클래스에서 JSON 스키마가 만들어짐(구조화 출력). */
public record AtomicDecomposition(
        @JsonPropertyDescription("분해된 원자 요구사항 목록. 원문에서 검증 가능한 조건을 찾을 수 없으면 빈 배열")
        List<Atomic> atomics) {

    public record Atomic(
            @JsonPropertyDescription("원자 요구사항 문장 (한 문장, 그 자체로 검증 가능한 단위로 쪼갤 것)")
            String atomicText,
            @JsonPropertyDescription("유형 — 금액 범위/비율 범위/기간·기한 조건/참거짓 플래그 중 가장 가까운 것")
            RequirementType type,
            @JsonPropertyDescription("범위형(AMOUNT_RANGE/RATE_RANGE)의 최소값. 해당 없으면 null")
            BigDecimal minValue,
            @JsonPropertyDescription("범위형의 최대값. 해당 없으면 null")
            BigDecimal maxValue,
            @JsonPropertyDescription("단위 (예: 원, %, 개월, 년, 세). 없으면 null")
            String unit,
            @JsonPropertyDescription("추가 조건 설명(값으로 다 표현 안 되는 것). 없으면 null")
            String conditions) {
    }
}
