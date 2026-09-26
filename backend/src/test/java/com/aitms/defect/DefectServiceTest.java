package com.aitms.defect;

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
import com.aitms.defect.DefectRequests.CommentRequest;
import com.aitms.defect.DefectRequests.DefectRequest;
import com.aitms.defect.DefectRequests.StatusChangeRequest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:defecttest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class DefectServiceTest {

    static final long PROJECT = 99L;       // 샘플 데이터와 분리된 테스트 전용 프로젝트
    static final long OTHER_PROJECT = 98L;

    @Autowired
    DefectService service;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO project (id, code, name) VALUES (99, 'TEST', '테스트'), (98, 'TEST2', '다른 프로젝트')");
    }

    private Defect create(String title, Long executionId) {
        return service.create(new DefectRequest(PROJECT, title, "재현 절차", Severity.MAJOR, Priority.HIGH, 2L, executionId));
    }

    private Long createExecution(long projectId) {
        jdbc.update("INSERT INTO test_case (tc_code, title) VALUES ('TC-T" + projectId + "', 'tc')");
        Long tcId = jdbc.queryForObject("SELECT id FROM test_case WHERE tc_code = 'TC-T" + projectId + "'", Long.class);
        jdbc.update("INSERT INTO test_cycle (id, project_id, cycle_no, name) VALUES (?, ?, 1, '1차')", 900 + projectId, projectId);
        jdbc.update("INSERT INTO test_execution (cycle_id, test_case_id, tc_version, result) VALUES (?, ?, 1, 'FAIL')",
                900 + projectId, tcId);
        return jdbc.queryForObject("SELECT id FROM test_execution WHERE cycle_id = ?", Long.class, 900 + projectId);
    }

    @Test
    void 등록하면_프로젝트내_코드가_채번되고_NEW_상태다() {
        Defect first = create("첫 결함", null);
        Defect second = create("두번째", null);

        assertThat(first.getDefectCode()).isEqualTo("DF-0001");
        assertThat(second.getDefectCode()).isEqualTo("DF-0002");
        assertThat(first.getStatus()).isEqualTo(DefectStatus.NEW);
        assertThat(first.getReporterName()).isEqualTo("김큐에이");
        assertThat(first.getAssigneeName()).isEqualTo("박개발");
        assertThat(first.getNextStatuses()).containsExactly(DefectStatus.OPEN, DefectStatus.REJECTED);
    }

    @Test
    void 수행항목에_연결하면_TC와_차수정보가_조인되고_실행ID로_조회된다() {
        Long execId = createExecution(PROJECT);
        Defect d = create("로그인 실패", execId);

        assertThat(d.getTcCode()).isEqualTo("TC-T99");
        assertThat(d.getCycleNo()).isEqualTo(1);

        DefectSearch search = new DefectSearch();
        search.setExecutionId(execId);
        assertThat(service.search(search).items()).extracting(Defect::getId).containsExactly(d.getId());
    }

    @Test
    void 다른_프로젝트의_수행항목에는_연결할_수_없다() {
        Long otherExec = createExecution(OTHER_PROJECT);

        assertThatThrownBy(() -> create("x", otherExec)).isInstanceOf(ApiException.class);
    }

    @Test
    void 상태는_허용된_흐름으로만_바뀌고_이력이_남는다() {
        Defect d = create("결함", null);

        assertThatThrownBy(() -> service.changeStatus(d.getId(), new StatusChangeRequest(DefectStatus.RESOLVED, null)))
                .isInstanceOf(ApiException.class);

        service.changeStatus(d.getId(), new StatusChangeRequest(DefectStatus.OPEN, "확인함"));
        service.changeStatus(d.getId(), new StatusChangeRequest(DefectStatus.IN_PROGRESS, null));
        service.addComment(d.getId(), new CommentRequest("원인 분석 중"));
        Defect resolved = service.changeStatus(d.getId(), new StatusChangeRequest(DefectStatus.RESOLVED, "수정 배포"));

        assertThat(resolved.getStatus()).isEqualTo(DefectStatus.RESOLVED);
        List<DefectComment> comments = service.comments(d.getId());
        assertThat(comments).extracting(DefectComment::getStatusTo)
                .containsExactly(DefectStatus.OPEN, DefectStatus.IN_PROGRESS, null, DefectStatus.RESOLVED);
        assertThat(comments.get(0).getStatusFrom()).isEqualTo(DefectStatus.NEW);
    }

    @Test
    void 미해결_필터는_NEW_OPEN_IN_PROGRESS만_조회한다() {
        create("미해결1", null);
        Defect rejected = create("반려", null);
        service.changeStatus(rejected.getId(), new StatusChangeRequest(DefectStatus.REJECTED, "중복"));

        DefectSearch search = new DefectSearch();
        search.setProjectId(PROJECT);
        search.setUnresolved(true);
        assertThat(service.search(search).items()).extracting(Defect::getTitle).containsExactly("미해결1");
    }
}
