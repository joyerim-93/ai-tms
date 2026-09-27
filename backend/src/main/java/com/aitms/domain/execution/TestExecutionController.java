package com.aitms.domain.execution;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aitms.domain.execution.ExecutionRequests.PatchRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class TestExecutionController {

    private final TestExecutionService service;

    @GetMapping("/{id}")
    public TestExecution get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/history")
    public List<ExecutionHistory> history(@PathVariable Long id) {
        return service.history(id);
    }

    /** 자동저장 — result/comment 모두 선택. result가 실제로 바뀔 때만 이력 1건 추가 */
    @PatchMapping("/{id}")
    public TestExecution patch(@PathVariable Long id, @Validated @RequestBody PatchRequest req) {
        return service.patch(id, req);
    }
}
