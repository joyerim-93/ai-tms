package com.aitms.requirement;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RequirementMapper {

    List<Requirement> findByProject(Long projectId);

    Optional<Requirement> findById(Long id);

    List<AtomicRequirement> findAtomics(Long requirementId);

    void insert(Requirement requirement);
}
