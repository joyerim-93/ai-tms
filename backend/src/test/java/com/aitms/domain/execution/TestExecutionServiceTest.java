package com.aitms.domain.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.Priority;
import com.aitms.domain.execution.ExecutionRequests.AddExecutionsRequest;
import com.aitms.domain.execution.ExecutionRequests.AssignRequest;
import com.aitms.domain.execution.ExecutionRequests.CycleRequest;
import com.aitms.domain.execution.ExecutionRequests.ResultRequest;
import com.aitms.domain.testcase.TestCaseRequest;
import com.aitms.domain.testcase.TestCaseService;
import com.aitms.domain.testcase.TestCaseStatus;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:exectest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class TestExecutionServiceTest {

    @Autowired
    TestCycleService cycleService;

    @Autowired
    TestExecutionService executionService;

    @Autowired
    TestCaseService testCaseService;

    static final Long PROJECT = 99L; // 샘플 데이터와 분리된 테스트 전용 프로젝트

    @Autowired
    JdbcTemplate jdbc;

    Long tc1;
    Long tc2;
    Long deprecated;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO project (id, code, name) VALUES (99, 'TEST', '테스트 프로젝트')");
        tc1 = createTc("TC1", TestCaseStatus.ACTIVE);
        tc2 = createTc("TC2", TestCaseStatus.ACTIVE);
        deprecated = createTc("폐기", TestCaseStatus.DEPRECATED);
    }

    private Long createTc(String title, TestCaseStatus status) {
        return testCaseService.create(new TestCaseRequest(PROJECT, null, title, null, null, Priority.MEDIUM, status, null, null, null, null, List.of())).getId();
    }

    private TestCycle createCycle(String name) {
        return cycleService.create(new CycleRequest(PROJECT, name, null, null, null));
    }

    @Test
    void 차수번호는_프로젝트내에서_순차_채번된다() {
        assertThat(createCycle("1차").getCycleNo()).isEqualTo(1);
        TestCycle second = createCycle("2차");
        assertThat(second.getCycleNo()).isEqualTo(2);
        assertThat(second.getStatus()).isEqualTo(CycleStatus.PLANNED);
    }

    @Test
    void TC등록시_중복과_폐기TC는_제외되고_현황이_집계된다() {
        TestCycle cycle = createCycle("1차");

        assertThat(executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(tc1, deprecated), 2L))).isEqualTo(1);
        assertThat(executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(tc1, tc2), null))).isEqualTo(1);

        TestCycle stats = cycleService.get(cycle.getId());
        assertThat(stats.getTotalCount()).isEqualTo(2);
        assertThat(stats.getNotRunCount()).isEqualTo(2);

        List<TestExecution> list = executionService.findByCycle(cycle.getId(), new ExecutionSearch());
        assertThat(list).extracting(TestExecution::getTcTitle).containsExactly("TC1", "TC2");
        assertThat(list.get(0).getAssigneeName()).isEqualTo("박개발");
    }

    @Test
    void 결과입력시_최종결과와_이력이_쌓이고_차수가_진행중으로_바뀐다() {
        TestCycle cycle = createCycle("1차");
        executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(tc1), null));
        Long execId = executionService.findByCycle(cycle.getId(), new ExecutionSearch()).get(0).getId();

        executionService.record(execId, new ResultRequest(ExecutionResult.FAIL, "버튼 미동작"));
        TestExecution exec = executionService.record(execId, new ResultRequest(ExecutionResult.PASS, null));

        assertThat(exec.getResult()).isEqualTo(ExecutionResult.PASS);
        assertThat(exec.getExecutedByName()).isEqualTo("김큐에이");
        assertThat(executionService.history(execId)).extracting(ExecutionHistory::getResult)
                .containsExactly(ExecutionResult.PASS, ExecutionResult.FAIL);

        TestCycle updated = cycleService.get(cycle.getId());
        assertThat(updated.getStatus()).isEqualTo(CycleStatus.IN_PROGRESS);
        assertThat(updated.getPassCount()).isEqualTo(1);

        // 이력이 있으면 항목 제외/차수 삭제 불가
        assertThatThrownBy(() -> executionService.remove(cycle.getId(), execId)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> cycleService.delete(cycle.getId())).isInstanceOf(ApiException.class);
    }

    @Test
    void 담당자를_일괄지정하고_결과로_필터링한다() {
        TestCycle cycle = createCycle("1차");
        executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(tc1, tc2), null));
        List<Long> ids = executionService.findByCycle(cycle.getId(), new ExecutionSearch()).stream()
                .map(TestExecution::getId).toList();

        assertThat(executionService.assign(cycle.getId(), new AssignRequest(ids, 3L))).isEqualTo(2);
        executionService.record(ids.get(0), new ResultRequest(ExecutionResult.BLOCKED, null));

        ExecutionSearch search = new ExecutionSearch();
        search.setAssigneeId(3L);
        search.setResult(ExecutionResult.NOT_RUN);
        assertThat(executionService.findByCycle(cycle.getId(), search)).hasSize(1);
    }

    @Test
    void 종료된_차수에는_결과를_입력할_수_없다() {
        TestCycle cycle = createCycle("1차");
        executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(tc1), null));
        Long execId = executionService.findByCycle(cycle.getId(), new ExecutionSearch()).get(0).getId();
        cycleService.update(cycle.getId(), new CycleRequest(null, "1차", null, null, CycleStatus.CLOSED));

        assertThatThrownBy(() -> executionService.record(execId, new ResultRequest(ExecutionResult.PASS, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("종료");
    }

    @Test
    void 미승인_TC와_다른_프로젝트_TC는_차수에_등록되지_않는다() {
        TestCycle cycle = createCycle("1차");
        jdbc.update("UPDATE test_case SET review_status = 'DRAFT' WHERE id = ?", tc2);
        // tc2(DRAFT), 샘플 TC 1(다른 프로젝트 소유) 제외
        assertThat(executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(tc1, tc2, 1L), null))).isEqualTo(1);
    }
}
