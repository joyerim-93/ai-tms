package com.aitms.attachment;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

/**
 * 첨부파일 API — /api/executions/{id}/attachments, /api/defects/{id}/attachments
 * (요청서의 test-round-cases 는 이 프로젝트에서 executions)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService service;

    // ── 테스트 수행 결과 증빙
    @GetMapping("/executions/{id}/attachments")
    public List<Attachment> listExecution(@PathVariable Long id) {
        return service.list(AttachmentTarget.EXECUTION, id);
    }

    @PostMapping(value = "/executions/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<Attachment> uploadExecution(@PathVariable Long id, @RequestParam("file") List<MultipartFile> files) {
        return service.upload(AttachmentTarget.EXECUTION, id, files);
    }

    @GetMapping("/executions/{id}/attachments/{attId}/file")
    public ResponseEntity<Resource> downloadExecution(@PathVariable Long id, @PathVariable Long attId,
                                                      @RequestParam(defaultValue = "false") boolean inline) {
        return file(service.download(AttachmentTarget.EXECUTION, id, attId), inline);
    }

    @DeleteMapping("/executions/{id}/attachments/{attId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExecution(@PathVariable Long id, @PathVariable Long attId) {
        service.delete(AttachmentTarget.EXECUTION, id, attId);
    }

    // ── 결함 첨부
    @GetMapping("/defects/{id}/attachments")
    public List<Attachment> listDefect(@PathVariable Long id) {
        return service.list(AttachmentTarget.DEFECT, id);
    }

    @PostMapping(value = "/defects/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<Attachment> uploadDefect(@PathVariable Long id, @RequestParam("file") List<MultipartFile> files) {
        return service.upload(AttachmentTarget.DEFECT, id, files);
    }

    @GetMapping("/defects/{id}/attachments/{attId}/file")
    public ResponseEntity<Resource> downloadDefect(@PathVariable Long id, @PathVariable Long attId,
                                                   @RequestParam(defaultValue = "false") boolean inline) {
        return file(service.download(AttachmentTarget.DEFECT, id, attId), inline);
    }

    @DeleteMapping("/defects/{id}/attachments/{attId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDefect(@PathVariable Long id, @PathVariable Long attId) {
        service.delete(AttachmentTarget.DEFECT, id, attId);
    }

    /** inline=true 는 이미지(썸네일/미리보기)에만 허용, 나머지는 항상 다운로드로 내려줌 */
    private static ResponseEntity<Resource> file(AttachmentService.Download d, boolean inline) {
        Attachment a = d.attachment();
        boolean showInline = inline && a.isImage();
        ContentDisposition disposition = (showInline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(a.getFileName(), java.nio.charset.StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(a.getContentType()))
                .contentLength(a.getFileSize())
                .body(d.resource());
    }
}
