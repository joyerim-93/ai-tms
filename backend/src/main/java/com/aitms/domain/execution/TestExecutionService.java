package com.aitms.domain.execution;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.CurrentUser;
import com.aitms.domain.execution.ExecutionRequests.AddExecutionsRequest;
import com.aitms.domain.execution.ExecutionRequests.AssignRequest;
import com.aitms.domain.execution.ExecutionRequests.ResultRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestExecutionService {

    private final TestExecutionMapper mapper;
    private final TestCycleService cycleService;

    public List<TestExecution> findByCycle(Long cycleId, ExecutionSearch search) {
        cycleService.get(cycleId);
        return mapper.findByCycle(cycleId, search);
    }

    public TestExecution get(Long id) {
        return mapper.findById(id)
                .orElseThrow(() -> ApiException.notFound("수행 항목을 찾을 수 없습니다. id=" + id));
    }

    public List<ExecutionHistory> history(Long id) {
        get(id);
        return mapper.findHistory(id);
    }

    /** @return 실제 등록 건수 (이미 등록된 TC·폐기 TC는 제외) */
    @Transactional
    public int add(Long cycleId, AddExecutionsRequest req) {
        cycleService.getOpen(cycleId);
        return mapper.insertAll(cycleId, req.testCaseIds(), req.assigneeId());
    }

    @Transactional
    public int assign(Long cycleId, AssignRequest req) {
        cycleService.getOpen(cycleId);
        return mapper.updateAssignee(cycleId, req.executionIds(), req.assigneeId());
    }

    /** 최종 결과 갱신 + 이력 1건 추가 */
    @Transactional
    public TestExecution record(Long id, ResultRequest req) {
        TestExecution exec = get(id);
        TestCycle cycle = cycleService.getOpen(exec.getCycleId());

        Long userId = CurrentUser.id();
        mapper.updateResult(id, req.result(), userId);

        ExecutionHistory history = new ExecutionHistory();
        history.setExecutionId(id);
        history.setResult(req.result());
        history.setComment(req.comment() == null || req.comment().isBlank() ? null : req.comment().strip());
        history.setExecutedBy(userId);
        mapper.insertHistory(history);

        cycleService.markInProgress(cycle);
        return get(id);
    }

    @Transactional
    public void remove(Long cycleId, Long id) {
        TestExecution exec = get(id);
        if (!exec.getCycleId().equals(cycleId)) {
            throw ApiException.notFound("해당 차수의 수행 항목이 아닙니다.");
        }
        cycleService.getOpen(cycleId);
        if (mapper.countHistory(id) > 0) {
            throw ApiException.conflict("수행 이력이 있는 항목은 차수에서 제외할 수 없습니다.");
        }
        mapper.delete(id);
    }
}
