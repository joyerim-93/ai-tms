package com.aitms.domain.execution;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.aitms.domain.project.Project;
import com.aitms.domain.project.ProjectService;

import lombok.RequiredArgsConstructor;

/**
 * 테스트수행 결과 엑셀 다운로드 — '이 차수만'(exportCycle) / '전체 차수'(exportProject, 맨 앞에 차수 컬럼 추가).
 * 컬럼: (차수) | TC Key | 제목 | 데이터셋 행 | 결과 | 담당자 | 실행일시 | 코멘트
 */
@Service
@RequiredArgsConstructor
public class TestExecutionExcelService {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] BASE_HEADERS =
            {"TC Key", "제목", "데이터셋 행", "결과", "담당자", "실행일시", "코멘트"};
    private static final int[] BASE_WIDTHS = {12, 40, 20, 10, 12, 18, 40};

    private final TestExecutionMapper mapper;
    private final TestCycleService cycleService;
    private final ProjectService projectService;

    public byte[] exportCycle(Long cycleId) {
        TestCycle cycle = cycleService.get(cycleId);
        List<TestExecution> rows = mapper.findByCycle(cycleId, new ExecutionSearch());
        String title = cycle.getCycleNo() + "차 " + cycle.getName();
        return build(title, BASE_HEADERS, BASE_WIDTHS, rows, false);
    }

    public byte[] exportProject(Long projectId) {
        Project project = projectService.get(projectId);
        List<TestExecution> rows = mapper.findByProject(projectId);
        String[] headers = prepend("차수", BASE_HEADERS);
        int[] widths = prepend(10, BASE_WIDTHS);
        return build(project.getName() + " 전체 차수", headers, widths, rows, true);
    }

    public String cycleFileName(Long cycleId) {
        TestCycle cycle = cycleService.get(cycleId);
        return cycle.getCycleNo() + "차_" + safe(cycle.getName()) + "_결과.xlsx";
    }

    public String projectFileName(Long projectId) {
        return safe(projectService.get(projectId).getName()) + "_전체차수_결과.xlsx";
    }

    private byte[] build(String sheetTitle, String[] headers, int[] widths, List<TestExecution> rows, boolean withCycle) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle head = wb.createCellStyle();
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet sheet = wb.createSheet(sheetName(sheetTitle));
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(head);
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 1);

            int r = 1;
            for (TestExecution e : rows) {
                Row row = sheet.createRow(r++);
                int col = 0;
                if (withCycle) {
                    row.createCell(col++).setCellValue(e.getCycleNo() + "차 " + e.getCycleName());
                }
                row.createCell(col++).setCellValue(e.getTcCode());
                row.createCell(col++).setCellValue(e.getTcTitle());
                row.createCell(col++).setCellValue(e.getDatasetLabel() == null ? "" : e.getDatasetLabel());
                row.createCell(col++).setCellValue(ExecutionLabels.result(e.getResult()));
                row.createCell(col++).setCellValue(e.getAssigneeName() == null ? "" : e.getAssigneeName());
                row.createCell(col++).setCellValue(e.getExecutedAt() == null ? "" : e.getExecutedAt().format(DATETIME));
                row.createCell(col).setCellValue(e.getLastComment() == null ? "" : e.getLastComment());
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("엑셀 생성 실패", ex);
        }
    }

    private static String[] prepend(String first, String[] rest) {
        String[] out = new String[rest.length + 1];
        out[0] = first;
        System.arraycopy(rest, 0, out, 1, rest.length);
        return out;
    }

    private static int[] prepend(int first, int[] rest) {
        int[] out = new int[rest.length + 1];
        out[0] = first;
        System.arraycopy(rest, 0, out, 1, rest.length);
        return out;
    }

    /** 엑셀 시트명 제한(31자, \\/?*[] 금지) */
    private static String sheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/?*\\[\\]]", " ").strip();
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private static String safe(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
