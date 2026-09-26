package com.aitms.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.Priority;
import com.aitms.defect.DefectRequests.DefectRequest;
import com.aitms.defect.DefectRequests.StatusChangeRequest;
import com.aitms.defect.DefectService;
import com.aitms.defect.DefectStatus;
import com.aitms.defect.Severity;
import com.aitms.execution.CycleStatus;
import com.aitms.execution.ExecutionRequests.AddExecutionsRequest;
import com.aitms.execution.ExecutionRequests.CycleRequest;
import com.aitms.execution.ExecutionRequests.ResultRequest;
import com.aitms.execution.ExecutionResult;
import com.aitms.execution.ExecutionSearch;
import com.aitms.execution.TestCycle;
import com.aitms.execution.TestCycleService;
import com.aitms.execution.TestExecutionService;
import com.aitms.testcase.TestCaseRequest;
import com.aitms.testcase.TestCaseService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:dashtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class DashboardServiceTest {

    static final long ME = 1L;     // CurrentUser (qa01)
    static final long OTHER = 2L;
    static final long P = 99L;     // 샘플 데이터와 분리된 테스트 전용 프로젝트

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO project (id, code, name) VALUES (99, 'TEST', '테스트')");
    }

    @Autowired DashboardService dashboard;
    @Autowired TestCaseService testCaseService;
    @Autowired TestCycleService cycleService;
    @Autowired TestExecutionService executionService;
    @Autowired DefectService defectService;

    private Long tc(String title) {
        return testCaseService.create(new TestCaseRequest(title, null, null, Priority.MEDIUM, null, null, null, null, List.of())).getId();
    }

    private TestCycle cycle(String name, List<Long> tcIds, Long assignee) {
        TestCycle c = cycleService.create(new CycleRequest(P, name, null, null, null));
        executionService.add(c.getId(), new AddExecutionsRequest(tcIds, assignee));
        return c;
    }

    private Long defect(Severity severity, Long assignee) {
        return defectService.create(new DefectRequest(P, "결함", null, severity, Priority.HIGH, assignee, null)).getId();
    }

    @Test
    void 데이터가_없으면_0과_빈목록() {
        DashboardSummary s = dashboard.summary(P);

        assertThat(s.myPendingExecutionCount()).isZero();
        assertThat(s.currentCycle()).isNull();
        assertThat(s.myExecutions()).isEmpty();
        assertThat(s.myDefects()).isEmpty();
    }

    @Test
    void 내_미수행_항목은_종료되지_않은_차수의_내_담당만_집계한다() {
        Long a = tc("A"), b = tc("B"), c = tc("C");
        TestCycle first = cycle("1차", List.of(a, b), ME);
        cycle("2차", List.of(c), OTHER);
        TestCycle closed = cycle("3차", List.of(a), ME);
        cycleService.update(closed.getId(), new CycleRequest(null, "3차", null, null, CycleStatus.CLOSED));

        Long doneId = executionService.findByCycle(first.getId(), new ExecutionSearch()).get(0).getId();
        executionService.record(doneId, new ResultRequest(ExecutionResult.FAIL, null));

        DashboardSummary s = dashboard.summary(P);

        assertThat(s.myPendingExecutionCount()).isEqualTo(1);
        assertThat(s.myExecutions()).extracting(MyExecution::getTcTitle).containsExactly("B");
        assertThat(s.myExecutions().get(0).getCycleName()).isEqualTo("1차");
        // 진행중(1차)이 계획(2차)보다 우선
        assertThat(s.currentCycle().getName()).isEqualTo("1차");
        assertThat(s.currentCycle().getFailCount()).isEqualTo(1);
    }

    @Test
    void 결함은_미해결만_집계하고_내_담당과_치명_건수를_구분한다() {
        defect(Severity.CRITICAL, ME);
        defect(Severity.MAJOR, OTHER);
        Long rejected = defect(Severity.CRITICAL, ME);
        defectService.changeStatus(rejected, new StatusChangeRequest(DefectStatus.REJECTED, null));

        DashboardSummary s = dashboard.summary(P);

        assertThat(s.unresolvedDefectCount()).isEqualTo(2);
        assertThat(s.criticalDefectCount()).isEqualTo(1);
        assertThat(s.myOpenDefectCount()).isEqualTo(1);
        assertThat(s.myDefects()).hasSize(1);
    }
}
