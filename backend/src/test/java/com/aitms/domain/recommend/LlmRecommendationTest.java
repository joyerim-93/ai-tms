package com.aitms.domain.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.domain.requirement.RecommendResponse;
import com.aitms.domain.requirement.RequirementMapper;
import com.aitms.domain.requirement.RequirementService;
import com.aitms.domain.testcase.ReviewStatus;
import com.aitms.domain.testcase.TcSource;
import com.aitms.domain.testcase.TestCase;
import com.aitms.domain.testcase.TestTechnique;

/** LLM 추천(3-3) — Claude API 는 가짜 LlmClient 로 대체 (요구사항 1: 샘플 원자 요구사항 1·2·3) */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:llmtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "app.ai.llm.enabled=true"})
@Transactional
class LlmRecommendationTest {

    @Autowired RequirementMapper requirementMapper;
    @Autowired RequirementService requirementService;
    @MockitoBean LlmClient llm;

    private LlmRecommendationService engine(boolean enabled, int max) {
        return new LlmRecommendationService(requirementMapper, llm, new LlmSettings(enabled, "test"), max);
    }

    private static LlmProposal.Case tc(long atomicId, String title) {
        return new LlmProposal.Case(atomicId, title, TestTechnique.EXPLORATORY,
                List.of(new LlmProposal.Step("최대 금액으로 최장 거치기간 가입", ""), new LlmProposal.Step("만기 이자 확인", "이자가 정확히 계산됨")));
    }

    @Test
    void 제안은_LLM_출처의_비파라미터화_후보로_변환되고_빈_기대결과는_null이_된다() throws Exception {
        when(llm.generate(any(), any(), eq(LlmProposal.class)))
                .thenReturn(new LlmProposal(List.of(tc(1L, "최대 금액 + 최장 거치기간 이자 정합성"))));

        RecommendationResult res = engine(true, 5).recommend(1L);

        assertThat(res.warnings()).isEmpty();
        assertThat(res.candidates()).singleElement().satisfies(c -> {
            assertThat(c.source()).isEqualTo(TcSource.LLM);
            assertThat(c.atomicRequirementId()).isEqualTo(1L);
            assertThat(c.parameterized()).isFalse();
            assertThat(c.datasetRows()).isEmpty();
            assertThat(c.technique()).isEqualTo(TestTechnique.EXPLORATORY);
            assertThat(c.steps()).extracting(TcRecommendation.Step::expectedResult).containsExactly(null, "이자가 정확히 계산됨");
        });
    }

    @Test
    void 프롬프트에는_원문_원자_요구사항_id_기존_TC_제목이_담긴다() throws Exception {
        when(llm.generate(any(), any(), eq(LlmProposal.class))).thenReturn(new LlmProposal(List.of()));
        engine(true, 3).recommend(1L);

        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(llm).generate(eq(LlmRecommendationService.SYSTEM), user.capture(), eq(LlmProposal.class));
        assertThat(user.getValue())
                .contains("REQ-001", "최소 1만원, 최대 300만원")            // 원문
                .contains("id=1 [AMOUNT_RANGE]", "id=3 [PERIOD_CONDITION]")   // 원자 요구사항
                .contains("가입금액 경계값 검증 (데이터 기반)")                  // 이미 있는 TC
                .contains("최대 3건");
    }

    @Test
    void 잘못된_제안은_걸러지고_최대_개수를_넘지_않는다() throws Exception {
        when(llm.generate(any(), any(), eq(LlmProposal.class))).thenReturn(new LlmProposal(List.of(
                tc(999L, "없는 원자 요구사항"),                                                    // 잘못된 id
                new LlmProposal.Case(1L, "  ", TestTechnique.EXPLORATORY, List.of(new LlmProposal.Step("a", "b"))), // 제목 없음
                new LlmProposal.Case(1L, "단계 없음", null, List.of()),                            // 단계 없음
                tc(1L, "유효 1"), tc(2L, "유효 2"), tc(3L, "유효 3"))));

        RecommendationResult res = engine(true, 2).recommend(1L);

        assertThat(res.candidates()).extracting(TcRecommendation::title).containsExactly("유효 1", "유효 2");
        assertThat(res.warnings()).singleElement().asString().contains("3건");
    }

    @Test
    void 호출이_실패하면_경고만_남기고_후보는_없다() throws Exception {
        when(llm.generate(any(), any(), eq(LlmProposal.class))).thenThrow(new IllegalStateException("인증 정보가 없습니다"));

        RecommendationResult res = engine(true, 5).recommend(1L);

        assertThat(res.candidates()).isEmpty();
        assertThat(res.warnings()).singleElement().asString().contains("건너뛰었습니다").contains("인증 정보가 없습니다");
    }

    @Test
    void 비활성이면_API를_호출하지_않고_비활성_경고를_남긴다() throws Exception {
        RecommendationResult res = engine(false, 5).recommend(1L);

        assertThat(res.candidates()).isEmpty();
        assertThat(res.warnings()).containsExactly(LlmRecommendationService.DISABLED_WARNING);
        verify(llm, never()).generate(any(), any(), any());
    }

    @Test
    void 재기동_없이_토글만_바꿔도_바로_반영된다() throws Exception {
        when(llm.generate(any(), any(), eq(LlmProposal.class)))
                .thenReturn(new LlmProposal(List.of(tc(1L, "토글 확인용"))));
        LlmSettings settings = new LlmSettings(false, "test");
        LlmRecommendationService service = new LlmRecommendationService(requirementMapper, llm, settings, 5);

        assertThat(service.recommend(1L).warnings()).containsExactly(LlmRecommendationService.DISABLED_WARNING);
        verify(llm, never()).generate(any(), any(), any());

        settings.setEnabled(true); // 같은 인스턴스에서 껐다 켜기만 함 — 서비스를 새로 만들지 않음
        RecommendationResult afterOn = service.recommend(1L);

        assertThat(afterOn.warnings()).isEmpty();
        assertThat(afterOn.candidates()).singleElement().satisfies(c -> assertThat(c.title()).isEqualTo("토글 확인용"));
        verify(llm).generate(any(), any(), eq(LlmProposal.class));
    }

    @Test
    void 추천_실행_시_LLM_TC가_DRAFT로_저장되고_요구사항에_연결된다() throws Exception {
        when(llm.generate(any(), any(), eq(LlmProposal.class)))
                .thenReturn(new LlmProposal(List.of(tc(1L, "최대 금액 + 최장 거치기간 이자 정합성 (AI)"))));

        RecommendResponse res = requirementService.recommend(1L);

        TestCase llmTc = res.created().stream().filter(t -> t.getSource() == TcSource.LLM).findFirst().orElseThrow();
        assertThat(llmTc.getReviewStatus()).isEqualTo(ReviewStatus.DRAFT);
        assertThat(llmTc.getProjectId()).isEqualTo(1L);
        assertThat(llmTc.getSteps()).hasSize(2);
        assertThat(llmTc.getRequirements()).extracting(r -> r.getAtomicRequirementId()).containsExactly(1L);
        // 규칙기반(RULE) 결과도 함께 생성됨
        assertThat(res.created()).extracting(TestCase::getSource).contains(TcSource.RULE);

        // 같은 제안이 다시 오면 중복 생성하지 않음
        assertThat(requirementService.recommend(1L).created()).extracting(TestCase::getSource).doesNotContain(TcSource.LLM);
    }
}
