package com.aitms.domain.dashboard;

import java.util.List;

import com.aitms.domain.defect.Defect;
import com.aitms.domain.execution.TestCycle;

/**
 * @param currentCycle 진행중 차수(없으면 가장 최근 계획 차수, 둘 다 없으면 null)
 * @param myExecutions 상위 {@value DashboardService#LIST_LIMIT}건, 전체 건수는 myPendingExecutionCount
 */
public record DashboardSummary(
        long myPendingExecutionCount,
        long myOpenDefectCount,
        long unresolvedDefectCount,
        long criticalDefectCount,
        TestCycle currentCycle,
        List<MyExecution> myExecutions,
        List<Defect> myDefects) {
}
