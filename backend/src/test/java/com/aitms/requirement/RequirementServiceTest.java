package com.aitms.requirement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.Priority;
import com.aitms.recommend.RuleCatalogService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reqtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class RequirementServiceTest {

    @Autowired
    RequirementService service;

    @Autowired
    RuleCatalogService ruleCatalogService;

    @Test
    void 샘플_요구사항은_원자요구사항과_연결TC수를_함께_조회한다() {
        Requirement req = service.get(1L);

        assertThat(req.getReqCode()).isEqualTo("REQ-001");
        assertThat(req.getAtomicCount()).isEqualTo(3);
        assertThat(req.getTestCaseCount()).isEqualTo(9);   // TC 1~7, 10, 11
        assertThat(req.getAtomics()).extracting(AtomicRequirement::getType)
                .containsExactly(RequirementType.AMOUNT_RANGE, RequirementType.RATE_RANGE, RequirementType.PERIOD_CONDITION);
        assertThat(req.getAtomics().get(0).getMaxValue()).isEqualByComparingTo("3000000");
    }

    @Test
    void 등록하면_프로젝트내_코드가_채번되고_MANUAL로_저장된다() {
        Requirement created = service.create(new RequirementRequest(1L, "중도해지 이율", "중도해지 시 기본금리의 50% 적용", null));

        assertThat(created.getReqCode()).isEqualTo("REQ-002");
        assertThat(created.getSource()).isEqualTo(RequirementSource.MANUAL);
        assertThat(created.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(service.findByProject(1L)).hasSize(2);
    }

    @Test
    void AI추천은_엔진_연동전까지_빈결과() {
        assertThat(service.recommend(1L)).isEmpty();
    }

    @Test
    void 규칙카탈로그는_타입별로_조회된다() {
        assertThat(ruleCatalogService.findAll()).hasSize(4);
        assertThat(ruleCatalogService.findByType(RequirementType.AMOUNT_RANGE)).singleElement()
                .satisfies(r -> assertThat(r.getTemplate()).contains("{min}"));
    }
}
