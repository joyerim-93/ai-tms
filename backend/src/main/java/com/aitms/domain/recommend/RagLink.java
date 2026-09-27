package com.aitms.domain.recommend;

import com.aitms.domain.requirement.RequirementType;

import lombok.Getter;
import lombok.Setter;

/** RagMapper.findLinks 행 — TC 가 검증하는 원자 요구사항 */
@Getter
@Setter
public class RagLink {
    private Long testCaseId;
    private String atomicText;
    private RequirementType type;
}
