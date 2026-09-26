package com.aitms.domain.dashboard;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.CurrentUser;
import com.aitms.common.PageResponse;
import com.aitms.domain.defect.Defect;
import com.aitms.domain.defect.DefectSearch;
import com.aitms.domain.defect.DefectService;
import com.aitms.domain.defect.Severity;
import com.aitms.domain.execution.CycleStatus;
import com.aitms.domain.execution.TestCycle;
import com.aitms.domain.execution.TestCycleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    static final int LIST_LIMIT = 10;
    static final int RECENT_DAYS = 7; // '이번 주' = 최근 7일

    private final DashboardMapper mapper;
    private final TestCycleService cycleService;
    private final DefectService defectService;

    public DashboardSummary summary(Long projectId) {
        Long me = CurrentUser.id();

        PageResponse<Defect> myDefects = unresolvedDefects(projectId, me, null, LIST_LIMIT);
        long unresolved = unresolvedDefects(projectId, null, null, 1).total();
        long critical = unresolvedDefects(projectId, null, Severity.CRITICAL, 1).total();

        List<TestCycle> cycles = cycleService.findByProject(projectId); // cycle_no 내림차순
        TestCycle current = currentCycle(cycles);

        return new DashboardSummary(
                mapper.countActiveTestCases(projectId, null),
                mapper.countActiveTestCases(projectId, RECENT_DAYS),
                current == null ? null : passRate(current),
                current == null ? null : previousPassRate(cycles, current),
                unresolved,
                mapper.countDefectsCreatedSince(projectId, RECENT_DAYS),
                cycles.stream().filter(c -> c.getStatus() != CycleStatus.CLOSED).count(),
                cycles.stream().filter(c -> c.getStatus() == CycleStatus.IN_PROGRESS).count(),
                current,
                mapper.countMyPendingExecutions(projectId, me),
                myDefects.total(),
                critical,
                mapper.findMyPendingExecutions(projectId, me, LIST_LIMIT),
                myDefects.items());
    }

    /** 진행중 차수 중 최신 → 없으면 계획 차수 중 최신 */
    private TestCycle currentCycle(List<TestCycle> cycles) {
        return cycles.stream()
                .filter(c -> c.getStatus() != CycleStatus.CLOSED)
                .min(Comparator.comparing((TestCycle c) -> c.getStatus() == CycleStatus.IN_PROGRESS ? 0 : 1)
                        .thenComparing(TestCycle::getCycleNo, Comparator.reverseOrder()))
                .orElse(null);
    }

    /** 현재 차수보다 번호가 작은 차수 중 최신이면서 수행 이력이 있는 차수의 통과율 */
    private Double previousPassRate(List<TestCycle> cycles, TestCycle current) {
        return cycles.stream()
                .filter(c -> c.getCycleNo() < current.getCycleNo())
                .map(DashboardService::passRate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** 통과율(%) = PASS / 수행완료(미수행 제외), 소수 1자리. 수행 0건이면 null */
    static Double passRate(TestCycle c) {
        int executed = c.getTotalCount() - c.getNotRunCount();
        if (executed == 0) {
            return null;
        }
        return Math.round(c.getPassCount() * 1000.0 / executed) / 10.0;
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
