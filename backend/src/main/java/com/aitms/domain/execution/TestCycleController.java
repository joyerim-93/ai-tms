package com.aitms.domain.execution;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aitms.domain.execution.ExecutionRequests.AddExecutionsRequest;
import com.aitms.domain.execution.ExecutionRequests.AssignRequest;
import com.aitms.domain.execution.ExecutionRequests.CycleRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cycles")
@RequiredArgsConstructor
public class TestCycleController {

    private final TestCycleService cycleService;
    private final TestExecutionService executionService;
    private final TestExecutionExcelService excelService;

    @GetMapping
    public List<TestCycle> list(@RequestParam Long projectId) {
        return cycleService.findByProject(projectId);
    }

    @GetMapping("/{id}")
    public TestCycle get(@PathVariable Long id) {
        return cycleService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCycle create(@Validated @RequestBody CycleRequest req) {
        return cycleService.create(req);
    }

    @PutMapping("/{id}")
    public TestCycle update(@PathVariable Long id, @Validated @RequestBody CycleRequest req) {
        return cycleService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cycleService.delete(id);
    }

    // ── 차수별 수행 항목

    @GetMapping("/{id}/executions")
    public List<TestExecution> executions(@PathVariable Long id, ExecutionSearch search) {
        return executionService.findByCycle(id, search);
    }

    @PostMapping("/{id}/executions")
    public Map<String, Integer> addExecutions(@PathVariable Long id,
                                              @Validated @RequestBody AddExecutionsRequest req) {
        return Map.of("added", executionService.add(id, req));
    }

    @PutMapping("/{id}/executions/assignee")
    public Map<String, Integer> assign(@PathVariable Long id, @Validated @RequestBody AssignRequest req) {
        return Map.of("updated", executionService.assign(id, req));
    }

    @DeleteMapping("/{id}/executions/{executionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeExecution(@PathVariable Long id, @PathVariable Long executionId) {
        executionService.remove(id, executionId);
    }

    // ── 엑셀 다운로드

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportCycle(@PathVariable Long id) {
        return xlsx(excelService.exportCycle(id), excelService.cycleFileName(id));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProject(@RequestParam Long projectId) {
        return xlsx(excelService.exportProject(projectId), excelService.projectFileName(projectId));
    }

    private static ResponseEntity<byte[]> xlsx(byte[] body, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
