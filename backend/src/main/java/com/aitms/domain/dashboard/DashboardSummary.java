package com.aitms.domain.dashboard;

import java.util.List;

import com.aitms.domain.defect.Defect;
import com.aitms.domain.execution.TestCycle;

/**
 * 대시보드 요약.
 *
 * @param totalTestCaseCount    사용(ACTIVE) TC 수 — 저장소는 프로젝트 비종속이라 전체 기준
 * @param testCasesAddedThisWeek 최근 7일 등록 TC 수
 * @param passRate              현재 차수 통과율(%) = PASS / 수행완료(미수행 제외), 수행 0건이면 null
 * @param previousPassRate      직전 차수(cycle_no 더 작은 것 중 최신) 통과율, 없으면 null
 * @param openDefectCount       미해결 이슈(NEW/OPEN/IN_PROGRESS) 수
 * @param defectsOpenedThisWeek 최근 7일 등록 이슈 수
 * @param activeCycleCount      종료되지 않은 차수 수
 * @param inProgressCycleCount  진행중 차수 수
 * @param currentCycle          진행중 차수(없으면 가장 최근 계획 차수, 둘 다 없으면 null)
 * @param myExecutions          (내 할일) 상위 {@value DashboardService#LIST_LIMIT}건
 */
public record DashboardSummary(
        long totalTestCaseCount,
        long testCasesAddedThisWeek,
        Double passRate,
        Double previousPassRate,
        long openDefectCount,
        long defectsOpenedThisWeek,
        long activeCycleCount,
        long inProgressCycleCount,
        TestCycle currentCycle,
        // 내 할일 (로그인 사용자 기준)
        long myPendingExecutionCount,
        long myOpenDefectCount,
        long criticalDefectCount,
        List<MyExecution> myExecutions,
        List<Defect> myDefects) {
}
