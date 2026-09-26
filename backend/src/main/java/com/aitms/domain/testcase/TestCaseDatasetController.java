package com.aitms.domain.testcase;

import java.util.List;

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

/** 파라미터화 TC 데이터셋 행 CRUD — 행 id가 차수 실행 항목에서 참조되므로 전체 교체가 아닌 행 단위 */
@RestController
@RequestMapping("/api/test-cases/{testCaseId}/datasets")
@RequiredArgsConstructor
public class TestCaseDatasetController {

    private final TestCaseDatasetService service;

    @GetMapping
    public List<TestCaseDataset> list(@PathVariable Long testCaseId) {
        return service.list(testCaseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseDataset create(@PathVariable Long testCaseId, @Validated @RequestBody DatasetRequest req) {
        return service.create(testCaseId, req);
    }

    @PutMapping("/{id}")
    public TestCaseDataset update(@PathVariable Long testCaseId, @PathVariable Long id,
                                  @Validated @RequestBody DatasetRequest req) {
        return service.update(testCaseId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long testCaseId, @PathVariable Long id) {
        service.delete(testCaseId, id);
    }
}
