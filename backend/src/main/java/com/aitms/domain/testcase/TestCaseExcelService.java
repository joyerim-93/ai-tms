package com.aitms.domain.testcase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.domain.project.ProjectService;

import lombok.RequiredArgsConstructor;

/**
 * 테스트케이스 엑셀 대량 업로드 / 템플릿.
 * <ul>
 *   <li>첫 시트, 첫 행=헤더. 컬럼은 헤더 이름으로 찾음(순서 무관): 제목 | 스텝 | 기대결과 | 폴더경로 | 기법</li>
 *   <li>제목·스텝·기대결과는 필수. 누락/형식 오류 행은 실패로 모으고 나머지는 저장(부분 성공)</li>
 *   <li>스텝·기대결과 셀은 줄바꿈으로 여러 단계 — "1. " 같은 번호는 제거. 줄 수가 같으면 단계별로 짝지어 넣고,
 *       다르면 기대결과 전체를 마지막 단계에 넣음(한 줄이면 마지막 단계 기대결과)</li>
 *   <li>폴더경로 "A &gt; B &gt; C" — 없는 폴더는 자동 생성, 비어 있으면 미분류</li>
 *   <li>기법: 한글 표시명(경계값 분석 등) 또는 영문 코드, 비어 있으면 없음</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TestCaseExcelService {

    static final String H_TITLE = "제목";
    static final String H_STEPS = "스텝";
    static final String H_EXPECTED = "기대결과";
    static final String H_FOLDER = "폴더경로";
    static final String H_TECHNIQUE = "기법";
    static final List<String> HEADERS = List.of(H_TITLE, H_STEPS, H_EXPECTED, H_FOLDER, H_TECHNIQUE);

    static final int MAX_ROWS = 1000;
    private static final Pattern STEP_NUMBER = Pattern.compile("^\\s*\\d+\\s*[.)]\\s*");
    private static final Map<String, TestTechnique> TECHNIQUE_BY_LABEL = Map.of(
            "경계값분석", TestTechnique.BOUNDARY_VALUE,
            "동등분할", TestTechnique.EQUIVALENCE_PARTITION,
            "디시전테이블", TestTechnique.DECISION_TABLE,
            "탐색적", TestTechnique.EXPLORATORY);

    private final TestCaseService testCaseService;
    private final TestCaseFolderService folderService;
    private final ProjectService projectService;

    /** 헤더만 있는 빈 양식 (+ 작성 안내 시트) */
    public byte[] template() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle head = wb.createCellStyle();
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle wrap = wb.createCellStyle();
            wrap.setWrapText(true);
            wrap.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);

            Sheet sheet = wb.createSheet("테스트케이스");
            Row header = sheet.createRow(0);
            int[] widths = {40, 50, 50, 36, 16};
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS.get(i));
                c.setCellStyle(head);
                sheet.setColumnWidth(i, widths[i] * 256);
                sheet.setDefaultColumnStyle(i, wrap);
            }
            sheet.createFreezePane(0, 1);

            Sheet guide = wb.createSheet("작성 안내");
            String[] lines = {
                    "• 첫 번째 시트('테스트케이스')의 1행 헤더는 그대로 두고 2행부터 작성합니다. (컬럼 순서는 바뀌어도 됩니다)",
                    "• 제목·스텝·기대결과는 필수입니다. 하나라도 비어 있는 행은 실패로 처리되고 나머지 행만 등록됩니다.",
                    "• 스텝/기대결과: 한 셀 안에서 줄바꿈(Alt+Enter)으로 여러 단계를 적습니다. '1. ' 같은 번호는 자동 제거됩니다.",
                    "  - 스텝과 기대결과의 줄 수가 같으면 단계별로 짝지어지고, 다르면 기대결과 전체가 마지막 단계에 들어갑니다.",
                    "• 폴더경로: 예) 금리 정책 > 거치기간별 금리  (없는 폴더는 자동 생성, 비우면 미분류)",
                    "• 기법(선택): 경계값 분석 / 동등 분할 / 디시전 테이블 / 탐색적",
                    "• 업로드된 테스트케이스는 '검토대기(DRAFT)' 상태로 등록되며, 승인해야 테스트 차수에 넣을 수 있습니다.",
                    "• 한 번에 최대 " + MAX_ROWS + "행, .xlsx 파일만 지원합니다.",
            };
            for (int i = 0; i < lines.length; i++) {
                guide.createRow(i).createCell(0).setCellValue(lines[i]);
            }
            guide.setColumnWidth(0, 120 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("템플릿 생성 실패", e);
        }
    }

    @Transactional
    public ExcelImportResult importExcel(Long projectId, InputStream in) {
        projectService.get(projectId); // 없으면 404
        List<ParsedRow> rows = parse(in);

        int success = 0;
        List<ExcelImportResult.Failure> failures = new ArrayList<>();
        Map<String, Long> folderCache = new HashMap<>();
        for (ParsedRow r : rows) {
            if (r.error != null) {
                failures.add(new ExcelImportResult.Failure(r.rowNumber, r.error));
                continue;
            }
            Long folderId = null;
            if (!r.folderPath.isEmpty()) {
                folderId = folderCache.computeIfAbsent(String.join("/", r.folderPath),
                        k -> folderService.findOrCreatePath(projectId, r.folderPath));
            }
            testCaseService.createUploaded(projectId, folderId, r.title, r.technique, r.steps);
            success++;
        }
        return new ExcelImportResult(success, failures.size(), failures);
    }

    // ── 파싱 ────────────────────────────────────────────────────────────────

    private static class ParsedRow {
        int rowNumber;
        String title;
        List<TestCaseRequest.StepRequest> steps;
        List<String> folderPath = List.of();
        TestTechnique technique;
        String error;
    }

    private List<ParsedRow> parse(InputStream in) {
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) {
                throw bad("헤더 행이 없습니다. 템플릿을 내려받아 작성해 주세요.");
            }
            DataFormatter fmt = new DataFormatter();
            Map<String, Integer> col = new HashMap<>();
            for (Cell c : header) {
                col.putIfAbsent(fmt.formatCellValue(c).replaceAll("\\s", ""), c.getColumnIndex());
            }
            List<String> missing = List.of(H_TITLE, H_STEPS, H_EXPECTED).stream().filter(h -> !col.containsKey(h)).toList();
            if (!missing.isEmpty()) {
                throw bad("헤더에 필수 컬럼이 없습니다: " + String.join(", ", missing) + " — 템플릿을 내려받아 작성해 주세요.");
            }

            List<ParsedRow> parsed = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String title = text(fmt, row, col.get(H_TITLE));
                String steps = text(fmt, row, col.get(H_STEPS));
                String expected = text(fmt, row, col.get(H_EXPECTED));
                String folder = text(fmt, row, col.get(H_FOLDER));
                String technique = text(fmt, row, col.get(H_TECHNIQUE));
                if (title.isEmpty() && steps.isEmpty() && expected.isEmpty() && folder.isEmpty() && technique.isEmpty()) {
                    continue; // 빈 행
                }
                if (parsed.size() >= MAX_ROWS) {
                    throw bad("한 번에 최대 " + MAX_ROWS + "행까지 업로드할 수 있습니다.");
                }
                parsed.add(parseRow(i + 1, title, steps, expected, folder, technique));
            }
            return parsed;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw bad("엑셀 파일(.xlsx)을 읽을 수 없습니다. 파일 형식을 확인해 주세요.");
        }
    }

    private ParsedRow parseRow(int rowNumber, String title, String steps, String expected, String folder, String technique) {
        ParsedRow r = new ParsedRow();
        r.rowNumber = rowNumber;

        List<String> missing = new ArrayList<>();
        if (title.isEmpty()) missing.add(H_TITLE);
        if (steps.isEmpty()) missing.add(H_STEPS);
        if (expected.isEmpty()) missing.add(H_EXPECTED);
        if (!missing.isEmpty()) {
            r.error = "필수값 누락: " + String.join(", ", missing);
            return r;
        }
        if (title.length() > 300) {
            r.error = "제목이 300자를 넘습니다.";
            return r;
        }
        List<String> actions = lines(steps);
        List<String> results = lines(expected);
        if (actions.isEmpty() || results.isEmpty()) {
            r.error = "스텝/기대결과에 내용이 없습니다.";
            return r;
        }
        if (actions.stream().anyMatch(s -> s.length() > 2000) || results.stream().anyMatch(s -> s.length() > 2000)) {
            r.error = "스텝/기대결과 한 단계가 2000자를 넘습니다.";
            return r;
        }
        if (!technique.isEmpty()) {
            r.technique = parseTechnique(technique);
            if (r.technique == null) {
                r.error = "알 수 없는 기법입니다: " + technique + " (경계값 분석 / 동등 분할 / 디시전 테이블 / 탐색적)";
                return r;
            }
        }
        if (!folder.isEmpty()) {
            List<String> path = new ArrayList<>();
            for (String seg : folder.split(">", -1)) {
                path.add(seg.strip());
            }
            if (path.stream().anyMatch(String::isEmpty) || path.stream().anyMatch(s -> s.length() > 200)) {
                r.error = "폴더경로 형식이 올바르지 않습니다: " + folder + " (예: 금리 정책 > 거치기간별 금리)";
                return r;
            }
            r.folderPath = path;
        }
        r.title = title;
        r.steps = pairSteps(actions, results);
        return r;
    }

    /** 줄 수가 같으면 단계별 짝, 아니면 기대결과 전체를 마지막 단계에 */
    static List<TestCaseRequest.StepRequest> pairSteps(List<String> actions, List<String> results) {
        List<TestCaseRequest.StepRequest> steps = new ArrayList<>();
        boolean paired = actions.size() == results.size();
        for (int i = 0; i < actions.size(); i++) {
            String exp = null;
            if (paired) {
                exp = results.get(i);
            } else if (i == actions.size() - 1) {
                exp = String.join("\n", results);
            }
            steps.add(new TestCaseRequest.StepRequest(actions.get(i), exp));
        }
        return steps;
    }

    private static List<String> lines(String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            String s = STEP_NUMBER.matcher(line).replaceFirst("").strip();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private static TestTechnique parseTechnique(String value) {
        String key = value.replaceAll("\\s", "");
        TestTechnique byLabel = TECHNIQUE_BY_LABEL.get(key);
        if (byLabel != null) {
            return byLabel;
        }
        try {
            return TestTechnique.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String text(DataFormatter fmt, Row row, Integer col) {
        if (row == null || col == null) {
            return "";
        }
        Cell c = row.getCell(col);
        return c == null ? "" : fmt.formatCellValue(c).strip();
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
