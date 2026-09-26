package com.aitms.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectMapper mapper;

    public List<Project> findAll() {
        return mapper.findAll();
    }

    public List<ProjectMember> members(Long projectId) {
        return mapper.findMembers(projectId);
    }
}
