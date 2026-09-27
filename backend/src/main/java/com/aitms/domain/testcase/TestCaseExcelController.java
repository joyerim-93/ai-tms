package com.aitms.domain.testcase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aitms.common.ApiException;

import lombok.RequiredArgsConstructor;

/** 테스트케이스 엑셀 대량 업로드 — JSON 'import'(다른 프로젝트에서 가져오기)와 같은 경로, multipart 로 구분 */
@RestController
@RequestMapping("/api/test-cases/import")
@RequiredArgsConstructor
public class TestCaseExcelController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final TestCaseExcelService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ExcelImportResult upload(@RequestParam Long projectId, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다.");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ".xlsx 파일만 업로드할 수 있습니다.");
        }
        return service.importExcel(projectId, file.getInputStream());
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("테스트케이스_업로드_템플릿.xlsx", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(XLSX))
                .body(service.template());
    }
}
