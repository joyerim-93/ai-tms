package com.aitms.domain.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.Priority;
import com.aitms.domain.project.Project;
import com.aitms.domain.project.ProjectRequest;
import com.aitms.domain.project.ProjectService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:foldertest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class TestCaseFolderServiceTest {

    @Autowired TestCaseFolderService folderService;
    @Autowired TestCaseService testCaseService;
    @Autowired ProjectService projectService;

    private TestCaseSearch search(Long projectId) {
        TestCaseSearch s = new TestCaseSearch();
        s.setProjectId(projectId);
        s.setSize(100);
        return s;
    }

    @Test
    void 샘플_폴더는_중첩_트리로_조립되고_하위포함_건수가_계산된다() {
        FolderTree tree = folderService.tree(1L);

        assertThat(tree.roots()).extracting(TestCaseFolder::getName).containsExactly("가입 프로세스", "금리 정책");
        TestCaseFolder rate = tree.roots().get(1);
        assertThat(rate.getChildren()).extracting(TestCaseFolder::getName).containsExactly("거치기간별 금리", "우대금리");
        assertThat(rate.getTestCaseCount()).isZero();
        assertThat(rate.getTotalCount()).isEqualTo(6);          // 4번 폴더 4건 + 5번 폴더 2건
        assertThat(tree.roots().get(0).getTotalCount()).isEqualTo(6); // 가입금액 검증: TC 1~4, 10, 14(파라미터화)
        assertThat(tree.totalCount()).isEqualTo(12);
        assertThat(tree.unfiledCount()).isZero();
    }

    @Test
    void 상위_폴더로_검색하면_하위_폴더_TC까지_조회되고_미분류는_따로_조회된다() {
        TestCaseSearch byParent = search(1L);
        byParent.setFolderId(2L); // 금리 정책
        assertThat(testCaseService.search(byParent).total()).isEqualTo(6);

        testCaseService.create(new TestCaseRequest(1L, null, "미분류 TC", null, null, Priority.LOW, null, null, null, null, null, List.of()));
        TestCaseSearch unfiled = search(1L);
        unfiled.setUnfiled(true);
        assertThat(testCaseService.search(unfiled).items()).extracting(TestCase::getTitle).containsExactly("미분류 TC");
    }

    @Test
    void 하위_폴더를_만들면_형제_중_마지막_순서가_되고_중복_이름과_다른_프로젝트_부모는_거부된다() {
        TestCaseFolder created = folderService.create(1L, new FolderRequest("중도해지 이율", 2L));

        assertThat(created.getParentFolderId()).isEqualTo(2L);
        assertThat(created.getSortOrder()).isEqualTo(3);
        assertThatThrownBy(() -> folderService.create(1L, new FolderRequest("우대금리", 2L)))
                .isInstanceOf(ApiException.class).hasMessageContaining("이미");
        assertThatThrownBy(() -> folderService.create(2L, new FolderRequest("x", 2L)))   // 2번 폴더는 프로젝트 1 소속
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 다른_프로젝트_폴더에는_TC를_넣을_수_없다() {
        assertThatThrownBy(() -> testCaseService.create(
                new TestCaseRequest(2L, 3L, "x", null, null, Priority.LOW, null, null, null, null, null, List.of())))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 다른_프로젝트에서_가져오면_새_row로_복제되고_원본_프로젝트가_기록된다() {
        // 프로젝트 2의 승인 TC 12, 13 / 같은 프로젝트(1)·미승인 TC 4는 제외
        List<TestCase> imported = testCaseService.importFrom(new ImportRequest(1L, List.of(12L, 13L, 4L, 1L), 5L));

        assertThat(imported).hasSize(2);
        TestCase copy = imported.get(0);
        assertThat(copy.getId()).isNotEqualTo(12L);
        assertThat(copy.getTcCode()).isNotEqualTo("TC-112");
        assertThat(copy.getProjectId()).isEqualTo(1L);
        assertThat(copy.getFolderName()).isEqualTo("우대금리");
        assertThat(copy.getOriginProjectId()).isEqualTo(2L);
        assertThat(copy.getOriginProjectName()).isEqualTo("KB 자유적금 갈아타기 이벤트");
        assertThat(copy.getRequirements()).isEmpty();            // 요구사항 링크는 복사 안 함
        assertThat(copy.getSteps()).hasSize(3);
        assertThat(testCaseService.get(12L).getProjectId()).isEqualTo(2L); // 원본은 그대로
    }

    @Test
    void 공통_테스트케이스_프로젝트로는_가져올_수_없지만_반대_방향은_허용된다() {
        // 프로젝트 3 = '공통 테스트케이스'(COMMON-TC) — 대상으로 가져오기 시도하면 거부
        assertThatThrownBy(() -> testCaseService.importFrom(new ImportRequest(3L, List.of(12L), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("공통 테스트케이스");

        // 반대 방향: 공통 테스트케이스(3)의 승인 TC를 일반 프로젝트(1)로 가져오는 것은 그대로 허용
        List<TestCase> imported = testCaseService.importFrom(new ImportRequest(1L, List.of(15L), null));
        assertThat(imported).singleElement().satisfies(tc -> assertThat(tc.getOriginProjectId()).isEqualTo(3L));
    }

    @Test
    void 가져오기_검색은_현재_프로젝트를_제외한_승인_TC만() {
        TestCaseSearch s = new TestCaseSearch();
        s.setExcludeProjectId(1L);
        s.setReviewStatus(ReviewStatus.APPROVED);
        s.setSize(50); // '공통 테스트케이스' 샘플까지 합쳐 기본 페이지(20)를 넘음

        assertThat(testCaseService.search(s).items()).extracting(TestCase::getProjectName)
                .containsOnly("KB 자유적금 갈아타기 이벤트", "공통 테스트케이스");
    }

    @Test
    void 프로젝트를_등록하면_코드는_대문자로_저장되고_등록자가_PM_멤버가_된다() {
        Project p = projectService.create(new ProjectRequest("open-bank", "오픈뱅킹 리뉴얼", null, null, null));

        assertThat(p.getCode()).isEqualTo("OPEN-BANK");
        assertThat(projectService.members(p.getId())).singleElement()
                .satisfies(m -> assertThat(m.getProjectRole()).isEqualTo("PM"));
        assertThatThrownBy(() -> projectService.create(new ProjectRequest("OPEN-BANK", "중복", null, null, null)))
                .isInstanceOf(ApiException.class);
        assertThat(folderService.tree(p.getId()).roots()).isEmpty();
    }

    @Test
    void 폴더_이름을_바꿀_수_있고_같은_위치_중복과_다른_프로젝트는_거부된다() {
        // 금리 정책(2)의 하위: 거치기간별 금리(4), 우대금리(5)
        TestCaseFolder renamed = folderService.rename(1L, 4L, new FolderRenameRequest("  기간별 금리  "));
        assertThat(renamed.getName()).isEqualTo("기간별 금리");
        assertThat(folderService.rename(1L, 4L, new FolderRenameRequest("기간별 금리")).getName()).isEqualTo("기간별 금리"); // 자기 이름 그대로는 허용

        assertThatThrownBy(() -> folderService.rename(1L, 4L, new FolderRenameRequest("우대금리")))
                .isInstanceOf(ApiException.class).hasMessageContaining("이미 있습니다");
        assertThatThrownBy(() -> folderService.rename(2L, 4L, new FolderRenameRequest("x")))
                .isInstanceOf(ApiException.class);
        assertThat(folderService.rename(1L, 3L, new FolderRenameRequest("금리 정책")).getName()).isEqualTo("금리 정책"); // 다른 부모 아래엔 같은 이름 가능
    }

    @Test
    void 가져오기_검색은_키워드를_프로젝트명에도_적용할_수_있다() {
        TestCaseSearch s = new TestCaseSearch();
        s.setExcludeProjectId(1L);
        s.setReviewStatus(ReviewStatus.APPROVED);
        s.setKeyword("자유적금");   // 프로젝트 2 이름에만 있고 TC 제목에는 없음
        assertThat(testCaseService.search(s).items()).isEmpty();

        s.setKeywordInProjectName(true);
        var res = testCaseService.search(s);
        assertThat(res.items()).extracting(TestCase::getProjectName).containsOnly("KB 자유적금 갈아타기 이벤트");
        assertThat(res.total()).isEqualTo(res.items().size());

        // 프로젝트 목록 화면의 일반 검색(프로젝트 고정)은 프로젝트명 조건을 켜지 않으면 영향 없음
        TestCaseSearch plain = search(2L);
        plain.setKeyword("적금");
        assertThat(testCaseService.search(plain).items()).isEmpty();
    }

    @Test
    void 폴더를_삭제하면_직속_하위_폴더는_부모로_승격되고_더_깊은_중첩은_유지된다() {
        // 금리 정책(2) 아래: 거치기간별 금리(4), 우대금리(5) — 금리 정책엔 직속 TC가 없음
        folderService.delete(1L, 2L);

        FolderTree tree = folderService.tree(1L);
        assertThat(tree.roots()).extracting(TestCaseFolder::getName).containsExactly("가입 프로세스", "거치기간별 금리", "우대금리");
        assertThat(testCaseService.get(5L).getFolderId()).isEqualTo(4L); // 더 안쪽 TC는 그대로
    }

    @Test
    void 폴더를_삭제하면_직속_TC는_부모_폴더로_이동한다() {
        // 우대금리(5, 부모=금리 정책 2)의 직속 TC 8·9 → 부모(2)로 이동
        folderService.delete(1L, 5L);
        assertThat(testCaseService.get(8L).getFolderId()).isEqualTo(2L);
        assertThat(testCaseService.get(9L).getFolderId()).isEqualTo(2L);
    }

    @Test
    void 최상위_폴더의_직속_TC는_삭제하면_미분류가_된다() {
        // 가입 프로세스(1, 최상위) 삭제 → 하위 폴더 '가입금액 검증'(3)이 최상위로 승격(TC는 그대로 폴더 3 소속)
        folderService.delete(1L, 1L);
        assertThat(testCaseService.get(1L).getFolderId()).isEqualTo(3L);

        // 이제 최상위가 된 폴더 3(직속 TC 1·2·3·4·10·14) 삭제 → 직속 TC는 미분류(NULL)
        int unfiledBefore = folderService.tree(1L).unfiledCount();
        folderService.delete(1L, 3L);
        assertThat(testCaseService.get(1L).getFolderId()).isNull();
        assertThat(folderService.tree(1L).unfiledCount()).isEqualTo(unfiledBefore + 6);
        assertThat(folderService.tree(1L).roots()).extracting(TestCaseFolder::getName).doesNotContain("가입금액 검증");
    }

    @Test
    void 다른_프로젝트의_폴더는_삭제할_수_없다() {
        assertThatThrownBy(() -> folderService.delete(2L, 1L)).isInstanceOf(ApiException.class);
    }
}
