package com.aitms.domain.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:exceltest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class TestCaseExcelServiceTest {

    @Autowired TestCaseExcelService service;
    @Autowired TestCaseService testCaseService;
    @Autowired TestCaseFolderService folderService;

    private static ByteArrayInputStream xlsx(List<String> header, List<List<String>> rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("테스트케이스");
            write(sheet.createRow(0), header);
            for (int i = 0; i < rows.size(); i++) {
                write(sheet.createRow(i + 1), rows.get(i));
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private static void write(Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != null) {
                row.createCell(i).setCellValue(values.get(i));
            }
        }
    }

    private static final List<String> HEADER = TestCaseExcelService.HEADERS;

    private TestCaseSearch search() {
        TestCaseSearch s = new TestCaseSearch();
        s.setProjectId(1L);
        s.setKeyword("[업로드]");
        s.setSize(100);
        return s;
    }

    @Test
    void 정상_행은_DRAFT_MANUAL로_저장되고_폴더가_자동_생성된다() throws Exception {
        ExcelImportResult res = service.importExcel(1L, xlsx(HEADER, List.of(
                List.of("[업로드] 금리 조회", "1. 상품 화면 진입\n2. 금리 확인", "1. 화면 표시\n2. 금리 2.5~3.9% 표시", "금리 정책 > 신규 폴더 > 하위", "경계값 분석"),
                List.of("[업로드] 폴더 재사용", "화면 진입", "정상 표시", "금리 정책 > 신규 폴더 > 하위", ""),
                List.of("[업로드] 미분류", "진입", "표시", "", ""))));

        assertThat(res.successCount()).isEqualTo(3);
        assertThat(res.failureCount()).isZero();

        List<TestCase> items = testCaseService.search(search()).items();
        assertThat(items).hasSize(3).allSatisfy(tc -> {
            assertThat(tc.getSource()).isEqualTo(TcSource.MANUAL);
            assertThat(tc.getReviewStatus()).isEqualTo(ReviewStatus.DRAFT);
            assertThat(tc.getStatus()).isEqualTo(TestCaseStatus.ACTIVE);
        });
        TestCase first = testCaseService.get(items.stream().filter(t -> t.getTitle().contains("금리 조회")).findFirst().orElseThrow().getId());
        assertThat(first.getTechnique()).isEqualTo(TestTechnique.BOUNDARY_VALUE);
        assertThat(first.getSteps()).extracting(TestStep::getAction).containsExactly("상품 화면 진입", "금리 확인");   // 번호 제거
        assertThat(first.getSteps()).extracting(TestStep::getExpectedResult).containsExactly("화면 표시", "금리 2.5~3.9% 표시"); // 줄 수 같으면 짝
        assertThat(first.getFolderName()).isEqualTo("하위");

        // 같은 경로 두 행 → 폴더는 한 번만 생성, 기존 '금리 정책'(2) 아래로 연결
        TestCaseFolder rate = folderService.tree(1L).roots().get(1);
        assertThat(rate.getName()).isEqualTo("금리 정책");
        assertThat(rate.getChildren()).extracting(TestCaseFolder::getName).contains("신규 폴더");
        TestCaseFolder created = rate.getChildren().stream().filter(f -> f.getName().equals("신규 폴더")).findFirst().orElseThrow();
        assertThat(created.getChildren()).singleElement().satisfies(c -> assertThat(c.getTestCaseCount()).isEqualTo(2));
        assertThat(items.stream().filter(t -> t.getFolderId() == null)).singleElement()
                .satisfies(t -> assertThat(t.getTitle()).contains("미분류"));
    }

    @Test
    void 필수값_누락_행은_실패로_행번호와_사유가_반환되고_나머지는_저장된다() throws Exception {
        ExcelImportResult res = service.importExcel(1L, xlsx(HEADER, List.of(
                List.of("[업로드] 정상", "진입", "표시", "", ""),                       // 2행
                List.of("", "진입", "표시", "", ""),                                     // 3행 제목 누락
                List.of("[업로드] 기대결과 없음", "진입", "", "", ""),                    // 4행
                List.of("", "", "", "", ""),                                            // 5행 빈 행 → 무시
                List.of("[업로드] 기법 오류", "진입", "표시", "", "마술"),                 // 6행
                List.of("[업로드] 폴더 오류", "진입", "표시", "A > > B", ""),              // 7행
                List.of("", "", "", "", ""),
                List.of("[업로드] 둘 다 없음", "", "", "", ""))));                        // 9행

        assertThat(res.successCount()).isEqualTo(1);
        assertThat(res.failureCount()).isEqualTo(5);
        assertThat(res.failures()).extracting(ExcelImportResult.Failure::row).containsExactly(3, 4, 6, 7, 9);
        assertThat(res.failures().get(0).reason()).isEqualTo("필수값 누락: 제목");
        assertThat(res.failures().get(1).reason()).isEqualTo("필수값 누락: 기대결과");
        assertThat(res.failures().get(2).reason()).contains("알 수 없는 기법");
        assertThat(res.failures().get(3).reason()).contains("폴더경로 형식");
        assertThat(res.failures().get(4).reason()).isEqualTo("필수값 누락: 스텝, 기대결과");
        assertThat(testCaseService.search(search()).items()).hasSize(1);
        // 실패한 행의 폴더는 만들어지지 않음
        assertThat(folderService.tree(1L).roots()).extracting(TestCaseFolder::getName).doesNotContain("A");
    }

    @Test
    void 스텝과_기대결과_줄_수가_다르면_기대결과_전체가_마지막_단계에_들어간다() {
        var steps = TestCaseExcelService.pairSteps(List.of("로그인", "가입 신청"), List.of("가입 완료"));
        assertThat(steps).extracting(TestCaseRequest.StepRequest::expectedResult).containsExactly(null, "가입 완료");
        var many = TestCaseExcelService.pairSteps(List.of("가입 신청"), List.of("완료 표시", "SMS 발송"));
        assertThat(many.get(0).expectedResult()).isEqualTo("완료 표시\nSMS 발송");
    }

    @Test
    void 헤더는_순서가_바뀌어도_이름으로_찾고_필수_헤더가_없으면_400() throws Exception {
        ExcelImportResult res = service.importExcel(1L, xlsx(List.of("기대결과", "제 목", "스텝"),
                List.of(List.of("표시", "[업로드] 순서 바뀜", "진입"))));
        assertThat(res.successCount()).isEqualTo(1);

        assertThatThrownBy(() -> service.importExcel(1L, xlsx(List.of("제목", "스텝"), List.of())))
                .isInstanceOf(ApiException.class).hasMessageContaining("기대결과");
        assertThatThrownBy(() -> service.importExcel(1L, new ByteArrayInputStream("not excel".getBytes())))
                .isInstanceOf(ApiException.class).hasMessageContaining(".xlsx");
        assertThatThrownBy(() -> service.importExcel(9999L, xlsx(HEADER, List.of())))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 템플릿은_헤더만_있는_빈_양식이고_다시_업로드하면_0건이다() throws Exception {
        byte[] bytes = service.template();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isZero();
            assertThat(java.util.stream.IntStream.range(0, 5).mapToObj(i -> sheet.getRow(0).getCell(i).getStringCellValue()).toList())
                    .containsExactly("제목", "스텝", "기대결과", "폴더경로", "기법");
        }
        ExcelImportResult res = service.importExcel(1L, new ByteArrayInputStream(bytes));
        assertThat(res.successCount()).isZero();
        assertThat(res.failureCount()).isZero();
    }
}
