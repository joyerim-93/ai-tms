package com.aitms.domain.requirement;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RequirementMapper {

    List<Requirement> findByProject(Long projectId);

    Optional<Requirement> findById(Long id);

    List<AtomicRequirement> findAtomics(Long requirementId);

    void insert(Requirement requirement);

    /** 원문 요구사항의 원자 요구사항별 커버 TC (atomicRequirementId로 그룹핑) */
    List<CoveringTestCase> findCoveringTestCases(Long requirementId);

    /** 프로젝트의 원자 요구사항 전체 (TC 폼 선택 목록) */
    List<AtomicRequirementRef> findAtomicRefsByProject(Long projectId);
}
