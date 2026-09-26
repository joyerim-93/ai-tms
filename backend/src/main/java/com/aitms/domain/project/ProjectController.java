package com.aitms.domain.project;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    @GetMapping
    public List<Project> list() {
        return service.findAll();
    }

    /** 프로젝트 등록 — 화면에서는 테스트케이스 탭에서만 노출 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@Validated @RequestBody ProjectRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}/members")
    public List<ProjectMember> members(@PathVariable Long id) {
        return service.members(id);
    }
}
