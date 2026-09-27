package com.aitms.domain.execution;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aitms.domain.execution.ExecutionRequests.DraftRequest;
import com.aitms.domain.execution.ExecutionRequests.ResultRequest;

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

    @PostMapping("/{id}/results")
    public TestExecution record(@PathVariable Long id, @Validated @RequestBody ResultRequest req) {
        return service.record(id, req);
    }

    /** 임시저장 — 확정 결과·이력에는 반영되지 않음 */
    @PostMapping("/{id}/draft")
    public TestExecution saveDraft(@PathVariable Long id, @Validated @RequestBody DraftRequest req) {
        return service.saveDraft(id, req);
    }
}
