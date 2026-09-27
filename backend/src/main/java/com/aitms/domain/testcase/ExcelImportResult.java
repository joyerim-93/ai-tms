package com.aitms.domain.testcase;

import java.util.List;

/**
 * 엑셀 업로드 결과.
 *
 * @param failures 실패한 행 — row 는 엑셀 행 번호(헤더=1행, 데이터는 2행부터)
 */
public record ExcelImportResult(int successCount, int failureCount, List<Failure> failures) {

    public record Failure(int row, String reason) {
    }
}
