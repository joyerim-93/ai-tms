package com.aitms.domain.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.Priority;
import com.aitms.domain.execution.ExecutionRequests.AddExecutionsRequest;
import com.aitms.domain.execution.ExecutionRequests.CycleRequest;
import com.aitms.domain.execution.ExecutionSearch;
import com.aitms.domain.execution.TestCycle;
import com.aitms.domain.execution.TestCycleService;
import com.aitms.domain.execution.TestExecution;
import com.aitms.domain.execution.TestExecutionService;
import com.aitms.domain.testcase.TestCaseRequest.StepRequest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:paramtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class ParameterizedTestCaseTest {

    @Autowired TestCaseService testCaseService;
    @Autowired TestCaseDatasetService datasetService;
    @Autowired TestCycleService cycleService;
    @Autowired TestExecutionService executionService;

    private TestCase parameterized(String title) {
        return testCaseService.create(new TestCaseRequest(1L, null, title, null, null, Priority.HIGH, null, null,
                TestTechnique.BOUNDARY_VALUE, true, null,
                List.of(new StepRequest("금리 {rate}% 입력", "{expected}"))));
    }

    private DatasetRequest row(String label, Map<String, Object> values, String expected) {
        return new DatasetRequest(label, values, expected);
    }

    @Test
    void 샘플_파라미터화_TC는_데이터셋_4행과_원본_JSON을_내려준다() {
        TestCase tc = testCaseService.get(14L);

        assertThat(tc.getTcCode()).isEqualTo("TC-114");
        assertThat(tc.getIsParameterized()).isTrue();
        assertThat(tc.getDatasets()).extracting(TestCaseDataset::getRowLabel).hasSize(4).first().asString().contains("9,999");
        assertThat(tc.getDatasets().get(0).getParamValues()).isEqualTo("{\"amount\": 9999}");
        assertThat(tc.getSteps().get(0).getAction()).isEqualTo("가입금액에 {amount}원 입력"); // 치환 전 원본
        assertThat(testCaseService.get(1L).getStatus()).isEqualTo(TestCaseStatus.DEPRECATED);   // 대체된 원본

        TestCaseSearch search = new TestCaseSearch();
        search.setProjectId(1L);
        search.setKeyword("데이터 기반");
        assertThat(testCaseService.search(search).items()).singleElement()
                .satisfies(t -> assertThat(t.getDatasetCount()).isEqualTo(4));
    }

    @Test
    void 데이터셋_행_CRUD와_변수명_검증() {
        TestCase tc = parameterized("금리 경계");
        TestCaseDataset r1 = datasetService.create(tc.getId(), row("최소", Map.of("rate", 2.5), "적용"));
        TestCaseDataset r2 = datasetService.create(tc.getId(), row("최대", Map.of("rate", 3.9), null));

        assertThat(r2.getSortOrder()).isEqualTo(r1.getSortOrder() + 1);
        assertThat(r1.getParamValues()).isEqualTo("{\"rate\":2.5}");

        datasetService.update(tc.getId(), r2.getId(), row("최대(수정)", Map.of("rate", 3.9, "기간", "3년"), "적용"));
        assertThat(datasetService.list(tc.getId())).extracting(TestCaseDataset::getRowLabel).containsExactly("최소", "최대(수정)");

        datasetService.delete(tc.getId(), r1.getId());
        assertThat(datasetService.list(tc.getId())).hasSize(1);

        assertThatThrownBy(() -> datasetService.create(tc.getId(), row("x", Map.of("bad name", 1), null)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> datasetService.create(tc.getId(), row("x", Map.of("nested", Map.of("a", 1)), null)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> datasetService.update(1L, r2.getId(), row("x", Map.of(), null)))  // 다른 TC의 행
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 파라미터화_TC를_차수에_등록하면_데이터셋_행마다_실행항목이_생기고_재등록시_새_행만_추가된다() {
        TestCase param = parameterized("금리 경계");
        datasetService.create(param.getId(), row("최소", Map.of("rate", 2.5), null));
        datasetService.create(param.getId(), row("최대", Map.of("rate", 3.9), null));
        TestCase plain = testCaseService.create(new TestCaseRequest(1L, null, "일반", null, null, Priority.LOW, null, null,
                null, null, null, List.of()));
        TestCycle cycle = cycleService.create(new CycleRequest(1L, "2차", null, null, null));

        assertThat(executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(param.getId(), plain.getId()), 1L)))
                .isEqualTo(3); // 데이터 2행 + 일반 1행

        List<TestExecution> rows = executionService.findByCycle(cycle.getId(), new ExecutionSearch());
        assertThat(rows).extracting(TestExecution::getDatasetLabel).containsExactly("최소", "최대", null);
        assertThat(rows.get(0).getDatasetParams()).isEqualTo("{\"rate\":2.5}");
        assertThat(rows.get(0).getTcParameterized()).isTrue();

        datasetService.create(param.getId(), row("초과", Map.of("rate", 4.0), null));
        assertThat(executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(param.getId(), plain.getId()), 1L)))
                .isEqualTo(1); // 새 데이터 행만

        // 실행 항목이 생긴 데이터 행은 삭제 불가
        Long usedRow = rows.get(0).getDatasetId();
        assertThatThrownBy(() -> datasetService.delete(param.getId(), usedRow)).isInstanceOf(ApiException.class);
    }

    @Test
    void 데이터셋_행이_없는_파라미터화_TC는_TC_단위로_등록된다() {
        TestCase param = parameterized("행 없음");
        TestCycle cycle = cycleService.create(new CycleRequest(1L, "2차", null, null, null));

        assertThat(executionService.add(cycle.getId(), new AddExecutionsRequest(List.of(param.getId()), null))).isEqualTo(1);
        assertThat(executionService.findByCycle(cycle.getId(), new ExecutionSearch()).get(0).getDatasetId()).isNull();
    }

    @Test
    void 실행_이력은_모든_차수의_결과를_시간_역순으로_데이터_행과_함께_보여준다() {
        List<TestCaseRun> runs = testCaseService.runs(14L);

        assertThat(runs).hasSize(4);
        assertThat(runs.get(0).getDatasetLabel()).startsWith("최대금액 초과");   // 가장 최근(10:15)
        assertThat(runs).filteredOn(r -> "FAIL".equals(r.getResult())).singleElement()
                .satisfies(r -> assertThat(r.getComment()).contains("DF-0001"));
        assertThat(runs.get(0).getCycleName()).isEqualTo("1차 통합테스트");
    }

    @Test
    void 연결된_요구사항_탭에서_링크만_교체한다() {
        assertThat(testCaseService.replaceRequirements(14L, List.of(1L, 2L)).getRequirements()).hasSize(2);
        assertThat(testCaseService.replaceRequirements(14L, List.of()).getRequirements()).isEmpty();
    }

    @Test
    void 파라미터화_TC를_다른_프로젝트로_가져오면_데이터셋도_복제된다() {
        List<TestCase> imported = testCaseService.importFrom(new ImportRequest(2L, List.of(14L), null));

        assertThat(imported).singleElement().satisfies(copy -> {
            assertThat(copy.getIsParameterized()).isTrue();
            assertThat(copy.getDatasets()).hasSize(4);
            assertThat(copy.getDatasets().get(0).getId()).isNotEqualTo(1L);
        });
    }
}
