package com.aitms.testcase;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aitms.common.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/test-cases")
@RequiredArgsConstructor
public class TestCaseController {

    private final TestCaseService service;

    @GetMapping
    public PageResponse<TestCase> search(TestCaseSearch search) {
        return service.search(search);
    }

    @GetMapping("/modules")
    public List<String> modules() {
        return service.modules();
    }

    @GetMapping("/{id}")
    public TestCase get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCase create(@Validated @RequestBody TestCaseRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public TestCase update(@PathVariable Long id, @Validated @RequestBody TestCaseRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/review")
    public TestCase review(@PathVariable Long id, @Validated @RequestBody TestCaseRequest.ReviewRequest req) {
        return service.review(id, req.reviewStatus());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
