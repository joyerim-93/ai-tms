package com.aitms.domain.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ragtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class RagRecommendationTest {

    /** 현재 프로젝트 97(요구사항 970 / 원자 970), 과거 프로젝트 96 */
    @Autowired RagRecommendationService rag;
    @Autowired RequirementService requirementService;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void data() {
        // 샘플 프로젝트(1·2)의 TC는 검색 대상에서 빼서 이 테스트 데이터만으로 검증
        jdbc.update("UPDATE test_case SET status = 'DEPRECATED'");
        jdbc.update("INSERT INTO project (id, code, name) VALUES (97, 'RAG-CUR', '현재'), (96, 'RAG-OLD', '과거')");
        jdbc.update("INSERT INTO requirement (id, project_id, req_code, title, description) VALUES "
                + "(970, 97, 'REQ-001', '우대금리', '원문'), (960, 96, 'REQ-001', '과거 우대금리', '원문')");
        jdbc.update("INSERT INTO atomic_requirement (id, requirement_id, atomic_text, type) VALUES "
                + "(970, 970, '가입 시 기존 고객에게 우대금리 0.3%p가 추가로 제공된다', 'BOOLEAN_FLAG'),"
                + "(960, 960, '기존 고객 갈아타기 시 우대금리 0.2%p가 가산된다', 'BOOLEAN_FLAG')");
        // 96: 검색 대상(APPROVED·ACTIVE, 파라미터화) / 초안 / 폐기 / 무관
        tc(960, 96, "우대금리 적용 여부 확인", "우대금리", "ACTIVE", "APPROVED", true);
        tc(961, 96, "우대금리 적용 여부 확인 (초안)", "우대금리", "ACTIVE", "DRAFT", false);
        tc(962, 96, "우대금리 적용 여부 확인 (폐기)", "우대금리", "DEPRECATED", "APPROVED", false);
        tc(963, 96, "로그인 화면 비밀번호 오류 안내", "로그인", "ACTIVE", "APPROVED", false);
        // 97: 현재 프로젝트의 비슷한 TC — 검색에서 제외되어야 함
        tc(970, 97, "우대금리 적용 여부 확인 (현재 프로젝트)", "우대금리", "ACTIVE", "APPROVED", false);
        jdbc.update("INSERT INTO test_case_requirement_link (test_case_id, atomic_requirement_id) VALUES (960, 960)");
        jdbc.update("INSERT INTO test_step (test_case_id, step_no, action, expected_result) VALUES "
                + "(960, 1, '[[flag]] = {flag} 인 고객으로 로그인', NULL), (960, 2, '가입 진행', '{expected}')");
        jdbc.update("INSERT INTO test_case_dataset (test_case_id, row_label, param_values, expected_result_override, sort_order) VALUES "
                + "(960, '기존 고객', '{\"flag\": true}', '우대금리 적용', 1), (960, '신규 고객', '{\"flag\": false}', '우대금리 미적용', 2)");
    }

    private void tc(long id, long projectId, String title, String module, String status, String review, boolean parameterized) {
        jdbc.update("INSERT INTO test_case (id, tc_code, project_id, title, module, status, source, technique, review_status, is_parameterized) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'MANUAL', 'EQUIVALENCE_PARTITION', ?, ?)",
                id, "RAG-" + id, projectId, title, module, status, review, parameterized);
    }

    @Test
    void 조사가_달라도_유사한_문장이_높은_점수를_받는다() {
        var a = TextSimilarity.features("우대금리가 추가된다");
        assertThat(TextSimilarity.dice(a, TextSimilarity.features("우대금리 가산"))).isGreaterThan(0.3);
        assertThat(TextSimilarity.dice(a, TextSimilarity.features("로그인 화면 비밀번호 오류"))).isLessThan(0.15);
        assertThat(TextSimilarity.dice(a, TextSimilarity.features(null))).isZero();
    }

    @Test
    void 다른_프로젝트의_승인된_활성_TC만_후보가_되고_원본_단계와_데이터셋을_복사한다() {
        List<TcRecommendation> candidates = rag.recommend(970L).candidates();

        assertThat(candidates).hasSize(1);   // 961(초안)·962(폐기)·963(무관)·970(현재 프로젝트) 제외
        TcRecommendation c = candidates.get(0);
        assertThat(c.source()).isEqualTo(TcSource.RAG);
        assertThat(c.originProjectId()).isEqualTo(96L);
        assertThat(c.atomicRequirementId()).isEqualTo(970L);
        assertThat(c.title()).isEqualTo("우대금리 적용 여부 확인");
        assertThat(c.score().doubleValue()).isBetween(KeywordTcRetriever.MIN_SCORE, 1.0);
        assertThat(c.steps()).extracting(TcRecommendation.Step::action)
                .containsExactly("[[flag]] = {flag} 인 고객으로 로그인", "가입 진행");
        assertThat(c.parameterized()).isTrue();
        assertThat(c.datasetRows()).extracting(TcRecommendation.DatasetRow::rowLabel).containsExactly("기존 고객", "신규 고객");
        assertThat(c.datasetRows().get(1).paramValues()).containsEntry("flag", false);
        assertThat(c.datasetRows().get(0).expectedResultOverride()).isEqualTo("우대금리 적용");
    }

    @Test
    void 추천을_실행하면_RAG_TC가_DRAFT로_저장되고_유사도가_응답에_담긴다_재실행은_건너뛴다() {
        RecommendResponse first = requirementService.recommend(970L);

        TestCase rag = first.created().stream().filter(t -> t.getSource() == TcSource.RAG).findFirst().orElseThrow();
        assertThat(rag.getProjectId()).isEqualTo(97L);
        assertThat(rag.getReviewStatus()).isEqualTo(ReviewStatus.DRAFT);
        assertThat(rag.getOriginProjectId()).isEqualTo(96L);
        assertThat(rag.getRequirements()).extracting(r -> r.getAtomicRequirementId()).containsExactly(970L);
        assertThat(rag.getDatasets()).hasSize(2);
        assertThat(first.scores()).containsKey(rag.getId());

        RecommendResponse again = requirementService.recommend(970L);
        assertThat(again.created()).isEmpty();
        assertThat(again.skipped()).isEqualTo(first.created().size());
    }

    @Test
    void 비슷한_TC가_없으면_RAG_후보는_없다() {
        jdbc.update("UPDATE atomic_requirement SET atomic_text = '전월 실적 기준 수수료 면제 조건' WHERE id = 970");
        assertThat(rag.recommend(970L).candidates()).isEmpty();
    }
}
