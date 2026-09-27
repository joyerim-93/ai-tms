package com.aitms.domain.recommend;

import java.util.ArrayList;
import java.util.List;

import com.aitms.domain.requirement.RequirementType;

import lombok.Getter;
import lombok.Setter;

/** RAG 검색 대상 TC(다른 프로젝트의 APPROVED·ACTIVE) + 그 TC가 검증하는 원자 요구사항들 */
@Getter
@Setter
public class RagCandidate {
    private Long id;
    private Long projectId;
    private String title;
    private String module;
    private String tags;

    private List<Linked> linked = new ArrayList<>();

    public record Linked(String atomicText, RequirementType type) {
    }
}
