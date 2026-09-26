package com.aitms.recommend;

import com.aitms.requirement.RequirementType;
import com.aitms.testcase.TestTechnique;

import lombok.Getter;
import lombok.Setter;

/** 요구사항 타입별 정형 테스트 기법 템플릿 ({min}, {max} 치환) */
@Getter
@Setter
public class RuleCatalog {
    private Long id;
    private RequirementType requirementType;
    private TestTechnique technique;
    private String template;
}
