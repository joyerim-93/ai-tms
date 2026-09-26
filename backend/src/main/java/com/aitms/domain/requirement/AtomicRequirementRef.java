package com.aitms.domain.requirement;

import lombok.Getter;
import lombok.Setter;

/** 원자 요구사항 참조 (원문 요구사항 코드·제목 포함) — TC 상세의 '검증하는 요구사항', TC 폼의 선택 목록 */
@Getter
@Setter
public class AtomicRequirementRef {
    private Long atomicRequirementId;
    private String atomicText;
    private RequirementType type;
    private Long requirementId;
    private String reqCode;
    private String requirementTitle;
}
