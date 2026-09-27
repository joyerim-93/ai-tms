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
import com.aitms.domain.execution.ExecutionRequests.PatchRequest;
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

        executionService.patch(execId, new PatchRequest(ExecutionResult.FAIL, "버튼 미동작"));
        TestExecution exec = executionService.patch(execId, new PatchRequest(ExecutionResult.PASS, null));

        assertThat(exec.getResult()).isEqualTo(ExecutionResult.PASS);
        assertThat(exec.getComment()).isNull(); // 마지막 patch가 null(=공백) 코멘트
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
    void 자동저장_결과가_안_바뀌면_코멘트만_갱신되고_이력은_안_쌓인다() {
        TestCycle cycle = createCycle("1차");
        executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(tc1), null));
        Long execId = executionService.findByCycle(cycle.getId(), new ExecutionSearch()).get(0).getId();

        // 결과 없이 코멘트만(디바운스 첫 틱: 아직 결과 선택 전) — NOT_RUN 그대로, 이력 없음
        TestExecution first = executionService.patch(execId, new PatchRequest(null, "작성 중"));
        assertThat(first.getResult()).isEqualTo(ExecutionResult.NOT_RUN);
        assertThat(first.getComment()).isEqualTo("작성 중");
        assertThat(executionService.history(execId)).isEmpty();

        // 코멘트만 다시 수정(결과는 그대로 NOT_RUN) — 여전히 이력 없음, 현재 값만 갱신
        TestExecution second = executionService.patch(execId, new PatchRequest(null, "다시 확인"));
        assertThat(second.getComment()).isEqualTo("다시 확인");
        assertThat(executionService.history(execId)).isEmpty();

        // 이제 결과가 실제로 바뀜 — 이 시점에야 이력 1건
        executionService.patch(execId, new PatchRequest(ExecutionResult.FAIL, "재현됨"));
        assertThat(executionService.history(execId)).hasSize(1);

        // 같은 결과로 다시 patch(코멘트만 수정) — 이력은 추가되지 않음
        executionService.patch(execId, new PatchRequest(ExecutionResult.FAIL, "재현됨, 스크린샷 첨부"));
        assertThat(executionService.history(execId)).hasSize(1);
    }

    @Test
    void 담당자를_일괄지정하고_결과로_필터링한다() {
        TestCycle cycle = createCycle("1차");
        executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(tc1, tc2), null));
        List<Long> ids = executionService.findByCycle(cycle.getId(), new ExecutionSearch()).stream()
                .map(TestExecution::getId).toList();

        assertThat(executionService.assign(cycle.getId(), new AssignRequest(ids, 3L))).isEqualTo(2);
        executionService.patch(ids.get(0), new PatchRequest(ExecutionResult.BLOCKED, null));

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

        assertThatThrownBy(() -> executionService.patch(execId, new PatchRequest(ExecutionResult.PASS, null)))
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
