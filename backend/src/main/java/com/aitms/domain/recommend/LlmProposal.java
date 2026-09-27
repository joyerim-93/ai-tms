package com.aitms.domain.recommend;

import java.util.List;

import com.aitms.domain.testcase.TestTechnique;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** LLM 이 돌려주는 TC 제안 — 이 클래스에서 JSON 스키마가 자동 생성됨(구조화 출력). */
public record LlmProposal(
        @JsonPropertyDescription("제안하는 테스트케이스 목록. 추가할 만한 것이 없으면 빈 배열") List<Case> cases) {

    public record Case(
            @JsonPropertyDescription("이 테스트케이스가 검증하는 원자 요구사항 id (주어진 목록 중 하나)") long atomicRequirementId,
            @JsonPropertyDescription("테스트케이스 제목 (한 줄, 무엇을 검증하는지)") String title,
            @JsonPropertyDescription("적용한 테스트 설계 기법") TestTechnique technique,
            @JsonPropertyDescription("수행 단계 (순서대로 2~6개)") List<Step> steps) {
    }

    public record Step(
            @JsonPropertyDescription("수행 절차") String action,
            @JsonPropertyDescription("기대 결과. 이 단계에서 확인할 것이 없으면 빈 문자열") String expectedResult) {
    }
}
