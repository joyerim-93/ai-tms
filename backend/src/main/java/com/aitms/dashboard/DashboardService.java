package com.aitms.dashboard;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.CurrentUser;
import com.aitms.common.PageResponse;
import com.aitms.defect.Defect;
import com.aitms.defect.DefectSearch;
import com.aitms.defect.DefectService;
import com.aitms.defect.Severity;
import com.aitms.execution.CycleStatus;
import com.aitms.execution.TestCycle;
import com.aitms.execution.TestCycleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    static final int LIST_LIMIT = 10;

    private final DashboardMapper mapper;
    private final TestCycleService cycleService;
    private final DefectService defectService;

    public DashboardSummary summary(Long projectId) {
        Long me = CurrentUser.id();

        PageResponse<Defect> myDefects = unresolvedDefects(projectId, me, null, LIST_LIMIT);
        long unresolved = unresolvedDefects(projectId, null, null, 1).total();
        long critical = unresolvedDefects(projectId, null, Severity.CRITICAL, 1).total();

        return new DashboardSummary(
                mapper.countMyPendingExecutions(projectId, me),
                myDefects.total(),
                unresolved,
                critical,
                currentCycle(projectId),
                mapper.findMyPendingExecutions(projectId, me, LIST_LIMIT),
                myDefects.items());
    }

    /** 진행중 차수 중 최신 → 없으면 계획 차수 중 최신 */
    private TestCycle currentCycle(Long projectId) {
        List<TestCycle> cycles = cycleService.findByProject(projectId);
        return cycles.stream()
                .filter(c -> c.getStatus() != CycleStatus.CLOSED)
                .min(Comparator.comparing((TestCycle c) -> c.getStatus() == CycleStatus.IN_PROGRESS ? 0 : 1)
                        .thenComparing(TestCycle::getCycleNo, Comparator.reverseOrder()))
                .orElse(null);
    }

    private PageResponse<Defect> unresolvedDefects(Long projectId, Long assigneeId, Severity severity, int size) {
        DefectSearch search = new DefectSearch();
        search.setProjectId(projectId);
        search.setAssigneeId(assigneeId);
        search.setSeverity(severity);
        search.setUnresolved(true);
        search.setSize(size);
        return defectService.search(search);
    }
}
