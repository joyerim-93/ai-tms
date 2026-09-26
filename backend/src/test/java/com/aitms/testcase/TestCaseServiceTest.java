package com.aitms.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.Priority;
import com.aitms.testcase.TestCaseRequest.StepRequest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:tctest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class TestCaseServiceTest {

    @Autowired
    TestCaseService service;

    @Autowired
    JdbcTemplate jdbc;

    private TestCaseRequest request(String title, String module, List<StepRequest> steps) {
        return new TestCaseRequest(title, module, "로그인 상태", Priority.HIGH, null, "login,smoke", null, null, steps);
    }

    @Test
    void 등록하면_코드가_채번되고_단계가_순서대로_저장된다() {
        TestCase tc = service.create(request("로그인 성공", "인증",
                List.of(new StepRequest("ID/PW 입력", null), new StepRequest("로그인 클릭", "메인 이동"))));

        assertThat(tc.getTcCode()).matches("TC-\\d{5}");
        assertThat(tc.getTcCode()).isGreaterThan("TC-00011"); // 샘플 코드 다음 번호
        assertThat(tc.getSource()).isEqualTo(TcSource.MANUAL);
        assertThat(tc.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(tc.getVersion()).isEqualTo(1);
        assertThat(tc.getStatus()).isEqualTo(TestCaseStatus.ACTIVE);
        assertThat(tc.getAuthorName()).isEqualTo("김큐에이");
        assertThat(tc.getSteps()).extracting(TestStep::getStepNo).containsExactly(1, 2);
        assertThat(tc.getSteps().get(1).getExpectedResult()).isEqualTo("메인 이동");
    }

    @Test
    void 수정하면_버전이_오르고_단계가_교체된다() {
        TestCase tc = service.create(request("원본", "인증", List.of(new StepRequest("a", null), new StepRequest("b", null))));

        TestCase updated = service.update(tc.getId(), request("수정본", "인증", List.of(new StepRequest("c", "ok"))));

        assertThat(updated.getTitle()).isEqualTo("수정본");
        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(updated.getSteps()).extracting(TestStep::getAction).containsExactly("c");
    }

    @Test
    void 키워드_모듈로_검색한다() {
        service.create(request("주문 취소", "주문", List.of()));
        service.create(request("주문 생성", "주문", List.of()));
        service.create(request("로그아웃", "인증", List.of()));

        TestCaseSearch search = new TestCaseSearch();
        search.setKeyword("주문");
        assertThat(service.search(search).total()).isEqualTo(2);

        search.setKeyword(null);
        search.setModule("인증");
        assertThat(service.search(search).items()).extracting(TestCase::getTitle).containsExactly("로그아웃");
        assertThat(service.modules()).contains("주문", "인증");
    }

    @Test
    void 차수에_등록된_TC는_삭제할_수_없다() {
        TestCase used = service.create(request("사용중", null, List.of()));
        TestCase free = service.create(request("미사용", null, List.of(new StepRequest("x", null))));
        jdbc.update("INSERT INTO test_cycle (id, project_id, cycle_no, name) VALUES (900, 1, 99, '테스트차수')");
        jdbc.update("INSERT INTO test_execution (cycle_id, test_case_id, tc_version) VALUES (900, ?, 1)", used.getId());

        assertThatThrownBy(() -> service.delete(used.getId())).isInstanceOf(ApiException.class);

        service.delete(free.getId());
        assertThatThrownBy(() -> service.get(free.getId())).isInstanceOf(ApiException.class);
    }

    @Test
    void AI추천_DRAFT_TC를_승인하면_검토자와_일시가_기록된다() {
        // 샘플 TC-00004: RULE 출처 DRAFT, 원자 요구사항(가입금액) 연결
        TestCase draft = service.get(4L);
        assertThat(draft.getReviewStatus()).isEqualTo(ReviewStatus.DRAFT);
        assertThat(draft.getSource()).isEqualTo(TcSource.RULE);
        assertThat(draft.getReqCode()).isEqualTo("REQ-001");
        assertThat(draft.getAtomicText()).contains("가입금액");

        TestCase approved = service.review(4L, ReviewStatus.APPROVED);

        assertThat(approved.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(approved.getReviewedByName()).isEqualTo("김큐에이");
        assertThat(approved.getReviewedAt()).isNotNull();
    }

    @Test
    void 출처와_검토상태로_검색한다() {
        TestCaseSearch search = new TestCaseSearch();
        search.setSource(TcSource.RAG);
        search.setReviewStatus(ReviewStatus.DRAFT);

        assertThat(service.search(search).items()).extracting(TestCase::getTcCode).containsExactly("TC-00009", "TC-00008");
        assertThat(service.search(search).items().get(0).getOriginProjectName()).isEqualTo("KB 자유적금 갈아타기 이벤트");
    }
}
