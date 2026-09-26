package com.aitms.domain.project;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProjectMapper {

    List<Project> findAll();

    Optional<Project> findById(Long id);

    int countByCode(String code);

    void insert(Project project);

    void insertMember(@Param("projectId") Long projectId,
                      @Param("userId") Long userId,
                      @Param("projectRole") String projectRole);

    List<ProjectMember> findMembers(Long projectId);
}
