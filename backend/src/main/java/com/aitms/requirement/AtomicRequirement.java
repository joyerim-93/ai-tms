package com.aitms.requirement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** AI 에이전트가 원문에서 분해한 원자 요구사항 */
@Getter
@Setter
public class AtomicRequirement {
    private Long id;
    private Long requirementId;
    private String atomicText;
    private RequirementType type;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String unit;
    private String conditions;   // JSON 문자열
    private LocalDateTime createdAt;

    private int testCaseCount;   // 연결된 TC 수
}
