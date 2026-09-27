package com.aitms.domain.testcase;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects/{projectId}/folders")
@RequiredArgsConstructor
public class TestCaseFolderController {

    private final TestCaseFolderService service;

    @GetMapping
    public FolderTree tree(@PathVariable Long projectId) {
        return service.tree(projectId);
    }

    @PutMapping("/{id}")
    public TestCaseFolder rename(@PathVariable Long projectId, @PathVariable Long id,
                                 @Validated @RequestBody FolderRenameRequest req) {
        return service.rename(projectId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long id) {
        service.delete(projectId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseFolder create(@PathVariable Long projectId, @Validated @RequestBody FolderRequest req) {
        return service.create(projectId, req);
    }
}
