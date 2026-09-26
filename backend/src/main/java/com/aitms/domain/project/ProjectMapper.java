package com.aitms.domain.project;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper {

    List<Project> findAll();

    List<ProjectMember> findMembers(Long projectId);
}
