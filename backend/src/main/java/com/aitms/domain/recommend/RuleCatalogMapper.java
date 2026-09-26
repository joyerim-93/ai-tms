package com.aitms.domain.recommend;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.aitms.domain.requirement.RequirementType;

@Mapper
public interface RuleCatalogMapper {

    List<RuleCatalog> findAll();

    List<RuleCatalog> findByType(RequirementType requirementType);
}
