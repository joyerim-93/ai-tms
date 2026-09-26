package com.aitms.domain.requirement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.Priority;
import com.aitms.domain.recommend.RuleCatalogService;

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
        assertThat(req.getTestCaseCount()).isEqualTo(10);  // TC 1~7, 10, 11, 14 (TC 10은 두 원자 요구사항에 걸쳐도 1건)
        assertThat(req.getCoveredAtomicCount()).isEqualTo(3);
        assertThat(req.getAtomics()).extracting(AtomicRequirement::getType)
                .containsExactly(RequirementType.AMOUNT_RANGE, RequirementType.RATE_RANGE, RequirementType.PERIOD_CONDITION);
        assertThat(req.getAtomics().get(0).getMaxValue()).isEqualByComparingTo("3000000");
        // Traceability: 거치기간 원자 요구사항(3)을 커버하는 TC
        assertThat(req.getAtomics().get(2).getTestCases()).extracting(CoveringTestCase::getTcCode)
                .containsExactly("TC-105", "TC-106", "TC-107", "TC-110");
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

    @Test
    void 프로젝트_원자요구사항_목록은_원문_코드와_함께_조회된다() {
        assertThat(service.atomicRefs(1L)).extracting(AtomicRequirementRef::getAtomicRequirementId).containsExactly(1L, 2L, 3L);
        assertThat(service.atomicRefs(1L).get(0).getReqCode()).isEqualTo("REQ-001");
        assertThat(service.atomicRefs(2L)).singleElement().satisfies(a -> assertThat(a.getAtomicRequirementId()).isEqualTo(4L));
    }
}
