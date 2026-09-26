package com.aitms.domain.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.domain.requirement.RecommendResponse;
import com.aitms.domain.requirement.RequirementService;
import com.aitms.domain.testcase.ReviewStatus;
import com.aitms.domain.testcase.TcSource;
import com.aitms.domain.testcase.TestCase;
import com.aitms.domain.testcase.TestCaseDataset;
import com.aitms.domain.testcase.TestTechnique;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ruletest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class RuleBasedRecommendationTest {

    @Autowired RuleBasedRecommendationService recommender;
    @Autowired RequirementService requirementService;
    @Autowired JdbcTemplate jdbc;

    private TcRecommendation byTechnique(List<TcRecommendation> list, Long atomicId) {
        return list.stream().filter(r -> r.atomicRequirementId().equals(atomicId)).findFirst().orElseThrow();
    }

    @Test
    void 금액_범위는_경계값_6행_파라미터화_TC로_생성된다() {
        TcRecommendation amount = byTechnique(recommender.recommend(1L).candidates(), 1L);

        assertThat(amount.parameterized()).isTrue();
        assertThat(amount.source()).isEqualTo(TcSource.RULE);
        assertThat(amount.technique()).isEqualTo(TestTechnique.BOUNDARY_VALUE);
        assertThat(amount.title()).startsWith("가입금액은 최소 1만원").endsWith("경계값 분석");
        assertThat(amount.steps().get(0).action()).isEqualTo("입력값에 {value}원 입력");  // [[unit]]만 생성 시 치환
        assertThat(amount.steps().get(1).expectedResult()).isEqualTo("{expected}");
        assertThat(amount.datasetRows()).extracting(r -> r.paramValues().get("value"))
                .containsExactly(9999L, 10000L, 10001L, 2999999L, 3000000L, 3000001L);
        assertThat(amount.datasetRows().get(0).rowLabel()).isEqualTo("최소값 미만 (9,999원)");
        assertThat(amount.datasetRows().get(5).expectedResultOverride()).contains("처리 거부");
    }

    @Test
    void 비율_범위는_step_001_단위_4행() {
        TcRecommendation rate = byTechnique(recommender.recommend(1L).candidates(), 2L);

        assertThat(rate.datasetRows()).extracting(r -> r.paramValues().get("value"))
                .containsExactly(new BigDecimal("2.49"), new BigDecimal("2.5"), new BigDecimal("3.9"), new BigDecimal("3.91"));
        assertThat(rate.datasetRows().get(1).rowLabel()).isEqualTo("최소값 (2.5%)");
    }

    @Test
    void 기간_조건은_선택지마다_1행_결정테이블과_매핑값() {
        TcRecommendation period = byTechnique(recommender.recommend(1L).candidates(), 3L);

        assertThat(period.technique()).isEqualTo(TestTechnique.DECISION_TABLE);
        assertThat(period.datasetRows()).extracting(TcRecommendation.DatasetRow::rowLabel).containsExactly("1년", "2년", "3년");
        assertThat(period.datasetRows().get(2).paramValues()).isEqualTo(Map.of("option", "3년", "mapped", new BigDecimal("3.9")));
        assertThat(period.datasetRows().get(2).expectedResultOverride()).isEqualTo("3년 선택 시 매핑 값 3.9 적용");
    }

    @Test
    void 여부_플래그는_동등분할_2행과_가산값() {
        TcRecommendation flag = recommender.recommend(2L).candidates().get(0);

        assertThat(flag.technique()).isEqualTo(TestTechnique.EQUIVALENCE_PARTITION);
        assertThat(flag.steps().get(0).action()).isEqualTo("existing_customer = {flag} 인 대상으로 진행");
        assertThat(flag.datasetRows()).extracting(r -> r.paramValues().get("flag")).containsExactly(true, false);
        assertThat(flag.datasetRows().get(0).expectedResultOverride()).isEqualTo("혜택 적용 (+0.2)");
    }

    @Test
    void 범위_값이_없으면_후보_대신_경고() {
        jdbc.update("UPDATE atomic_requirement SET min_value = NULL, max_value = NULL WHERE id = 1");

        RecommendationResult result = recommender.recommend(1L);

        assertThat(result.candidates()).extracting(TcRecommendation::atomicRequirementId).doesNotContain(1L);
        assertThat(result.warnings()).singleElement().asString().contains("#1");
    }

    @Test
    void 추천_실행은_DRAFT_파라미터화_TC를_저장하고_재실행하면_건너뛴다() {
        RecommendResponse first = requirementService.recommend(1L);

        assertThat(first.created()).hasSize(3);
        TestCase amount = first.created().stream()
                .filter(t -> t.getTitle().endsWith("경계값 분석") && t.getTitle().startsWith("가입금액")).findFirst().orElseThrow();
        assertThat(amount.getReviewStatus()).isEqualTo(ReviewStatus.DRAFT);
        assertThat(amount.getIsParameterized()).isTrue();
        assertThat(amount.getProjectId()).isEqualTo(1L);
        assertThat(amount.getFolderId()).isNull();                 // 미분류
        assertThat(amount.getAuthorId()).isNull();                 // 시스템 생성
        assertThat(amount.getRequirements()).extracting(r -> r.getAtomicRequirementId()).containsExactly(1L);
        assertThat(amount.getDatasets()).extracting(TestCaseDataset::getParamValues).first().isEqualTo("{\"value\":9999}");
        assertThat(amount.getSteps()).hasSize(2);

        RecommendResponse again = requirementService.recommend(1L);
        assertThat(again.created()).isEmpty();
        assertThat(again.skipped()).isEqualTo(3);
    }
}
