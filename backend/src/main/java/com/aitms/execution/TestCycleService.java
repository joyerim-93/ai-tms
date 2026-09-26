package com.aitms.execution;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.execution.ExecutionRequests.CycleRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestCycleService {

    private final TestCycleMapper mapper;

    public List<TestCycle> findByProject(Long projectId) {
        return mapper.findByProject(projectId);
    }

    public TestCycle get(Long id) {
        return mapper.findById(id)
                .orElseThrow(() -> ApiException.notFound("테스트 차수를 찾을 수 없습니다. id=" + id));
    }

    /** CLOSED 차수는 TC 등록/결과 입력 불가 */
    public TestCycle getOpen(Long id) {
        TestCycle cycle = get(id);
        if (cycle.getStatus() == CycleStatus.CLOSED) {
            throw ApiException.conflict("종료된 차수입니다. 상태를 변경한 뒤 진행하세요.");
        }
        return cycle;
    }

    @Transactional
    public TestCycle create(CycleRequest req) {
        if (req.projectId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "projectId: 필수 값입니다.");
        }
        TestCycle cycle = apply(new TestCycle(), req);
        cycle.setProjectId(req.projectId());
        cycle.setStatus(CycleStatus.PLANNED);
        mapper.insert(cycle);
        return get(cycle.getId());
    }

    @Transactional
    public TestCycle update(Long id, CycleRequest req) {
        if (req.status() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status: 필수 값입니다.");
        }
        TestCycle cycle = apply(get(id), req);
        cycle.setStatus(req.status());
        mapper.update(cycle);
        return get(id);
    }

    /** 수행 이력이 하나라도 있으면 삭제 불가 → 종료 처리 유도 */
    @Transactional
    public void delete(Long id) {
        get(id);
        if (mapper.countHistory(id) > 0) {
            throw ApiException.conflict("수행 이력이 있는 차수는 삭제할 수 없습니다. 종료 처리하세요.");
        }
        mapper.delete(id);
    }

    /** 첫 결과 입력 시 PLANNED → IN_PROGRESS 자동 전환 */
    @Transactional
    public void markInProgress(TestCycle cycle) {
        if (cycle.getStatus() == CycleStatus.PLANNED) {
            mapper.updateStatus(cycle.getId(), CycleStatus.IN_PROGRESS);
        }
    }

    private TestCycle apply(TestCycle cycle, CycleRequest req) {
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "종료일이 시작일보다 빠릅니다.");
        }
        cycle.setName(req.name().strip());
        cycle.setStartDate(req.startDate());
        cycle.setEndDate(req.endDate());
        return cycle;
    }
}
