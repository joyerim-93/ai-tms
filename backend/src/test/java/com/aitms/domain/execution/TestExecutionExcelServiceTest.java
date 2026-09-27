package com.aitms.domain.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 테스트수행 결과 엑셀 다운로드 — 샘플 데이터(프로젝트 1, 1차 통합테스트) 기준 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:exectest2;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class TestExecutionExcelServiceTest {

    @Autowired TestExecutionExcelService excelService;

    private static List<List<String>> readRows(byte[] bytes) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            return StreamSupport.stream(sheet.spliterator(), false)
                    .map(row -> StreamSupport.stream(row.spliterator(), false)
                            .map(TestExecutionExcelServiceTest::text).collect(Collectors.toList()))
                    .collect(Collectors.toList());
        }
    }

    private static String text(org.apache.poi.ss.usermodel.Cell c) {
        return new org.apache.poi.ss.usermodel.DataFormatter().formatCellValue(c);
    }

    @Test
    void 차수_단위_다운로드는_TC_Key_제목_데이터셋_결과_담당자_실행일시_코멘트_컬럼이다() throws Exception {
        List<List<String>> rows = readRows(excelService.exportCycle(1L));

        assertThat(rows.get(0)).containsExactly("TC Key", "제목", "데이터셋 행", "결과", "담당자", "실행일시", "코멘트");
        List<String> tc101 = rows.stream().filter(r -> r.get(0).equals("TC-101")).findFirst().orElseThrow();
        assertThat(tc101.get(3)).isEqualTo("성공");
        assertThat(tc101.get(4)).isEqualTo("김큐에이");
        assertThat(tc101.get(5)).contains("2026-09-22");

        // 파라미터화 TC(TC-114)는 데이터셋 행마다 한 행씩, '데이터셋 행' 컬럼에 라벨이 들어감
        List<List<String>> tc114Rows = rows.stream().filter(r -> r.get(0).equals("TC-114")).toList();
        assertThat(tc114Rows).hasSize(4);
        assertThat(tc114Rows.get(0).get(2)).contains("최소금액");
        // 파라미터화 아닌 TC는 데이터셋 행이 비어있음
        assertThat(tc101.get(2)).isEmpty();
    }

    @Test
    void 프로젝트_단위_다운로드는_맨_앞에_차수_컬럼이_붙고_모든_차수를_포함한다() throws Exception {
        List<List<String>> rows = readRows(excelService.exportProject(1L));

        assertThat(rows.get(0)).containsExactly("차수", "TC Key", "제목", "데이터셋 행", "결과", "담당자", "실행일시", "코멘트");
        List<String> tc101 = rows.stream().filter(r -> r.get(1).equals("TC-101")).findFirst().orElseThrow();
        assertThat(tc101.get(0)).contains("1차");
    }

    @Test
    void 파일명은_차수번호_이름_프로젝트명을_담고_경로_구분자가_없다() {
        assertThat(excelService.cycleFileName(1L)).isEqualTo("1차_1차 통합테스트_결과.xlsx");
        assertThat(excelService.projectFileName(1L)).doesNotContain("/", "\\");
    }
}
