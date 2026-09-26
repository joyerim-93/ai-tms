package com.aitms.recommend;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.aitms.requirement.RequirementType;

@Mapper
public interface RuleCatalogMapper {

    List<RuleCatalog> findAll();

    List<RuleCatalog> findByType(RequirementType requirementType);
}
