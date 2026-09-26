package com.aitms.domain.testcase;

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
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<String> modules(@RequestParam(required = false) Long projectId) {
        return service.modules(projectId);
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

    /** 다른 프로젝트에서 가져오기 — 선택한 TC를 현재 프로젝트에 복제 */
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TestCase> importFrom(@Validated @RequestBody ImportRequest req) {
        return service.importFrom(req);
    }

    /** 연결된 요구사항 교체 (다대다) */
    @PutMapping("/{id}/requirements")
    public TestCase replaceRequirements(@PathVariable Long id, @RequestBody RequirementLinkRequest req) {
        return service.replaceRequirements(id, req.atomicRequirementIds());
    }

    /** 실행 이력 — 이 TC가 각 차수에서 받은 결과 */
    @GetMapping("/{id}/runs")
    public List<TestCaseRun> runs(@PathVariable Long id) {
        return service.runs(id);
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
