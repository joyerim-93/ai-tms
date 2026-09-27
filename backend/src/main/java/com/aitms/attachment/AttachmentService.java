package com.aitms.attachment;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.aitms.common.ApiException;
import com.aitms.common.CurrentUser;
import com.aitms.domain.defect.DefectService;
import com.aitms.domain.execution.TestCycleService;
import com.aitms.domain.execution.TestExecution;
import com.aitms.domain.execution.TestExecutionService;

import lombok.RequiredArgsConstructor;

/**
 * 증빙 첨부 — 실행 항목(스크린샷/로그)과 결함(재현 스크린샷) 공용.
 * 규칙: 대상이 없으면 404, CLOSED 차수의 실행 항목은 추가/삭제 409(결과 입력과 동일), 항목당 최대 20개,
 * 삭제는 업로드한 사람 또는 ADMIN 만, 파일은 디스크·DB에는 상대 경로만.
 */
@Service
@RequiredArgsConstructor
public class AttachmentService {

    static final int MAX_PER_OWNER = 20;

    private final AttachmentMapper mapper;
    private final FileStorage storage;
    private final TestExecutionService executionService;
    private final TestCycleService cycleService;
    private final DefectService defectService;

    public List<Attachment> list(AttachmentTarget target, Long ownerId) {
        assertOwner(target, ownerId, false);
        return mapper.findByOwner(target, ownerId);
    }

    @Transactional
    public List<Attachment> upload(AttachmentTarget target, Long ownerId, List<MultipartFile> files) {
        assertOwner(target, ownerId, true);
        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "첨부할 파일을 선택해 주세요.");
        }
        if (mapper.countByOwner(target, ownerId) + files.size() > MAX_PER_OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "첨부파일은 항목당 최대 " + MAX_PER_OWNER + "개까지 가능합니다.");
        }
        List<Attachment> saved = new ArrayList<>();
        List<String> written = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                FileStorage.Stored stored = storage.store(target.directory() + "/" + ownerId, file);
                written.add(stored.path());
                Attachment a = new Attachment();
                a.setOwnerId(ownerId);
                a.setFileName(stored.fileName());
                a.setFilePath(stored.path());
                a.setContentType(stored.contentType());
                a.setFileSize(stored.size());
                a.setUploadedBy(CurrentUser.id());
                mapper.insert(target, a);
                saved.add(a);
            }
        } catch (RuntimeException e) {
            written.forEach(storage::deleteQuietly); // 일부만 저장된 채 남지 않게 정리
            throw e;
        }
        return saved.stream().map(a -> mapper.findById(target, ownerId, a.getId()).orElseThrow()).toList();
    }

    public record Download(Attachment attachment, Resource resource) {
    }

    public Download download(AttachmentTarget target, Long ownerId, Long id) {
        assertOwner(target, ownerId, false);
        Attachment a = get(target, ownerId, id);
        return new Download(a, storage.load(a.getFilePath()));
    }

    @Transactional
    public void delete(AttachmentTarget target, Long ownerId, Long id) {
        assertOwner(target, ownerId, true);
        Attachment a = get(target, ownerId, id);
        if (!a.getUploadedBy().equals(CurrentUser.id()) && !CurrentUser.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "업로드한 사람 또는 관리자만 삭제할 수 있습니다.");
        }
        mapper.delete(target, id);
        storage.deleteQuietly(a.getFilePath());
    }

    private Attachment get(AttachmentTarget target, Long ownerId, Long id) {
        return mapper.findById(target, ownerId, id)
                .orElseThrow(() -> ApiException.notFound("첨부파일을 찾을 수 없습니다. id=" + id));
    }

    /** 대상 존재 확인 (+ 쓰기면 종료된 차수 잠금) */
    private void assertOwner(AttachmentTarget target, Long ownerId, boolean write) {
        switch (target) {
            case EXECUTION -> {
                TestExecution exec = executionService.get(ownerId);
                if (write) {
                    cycleService.getOpen(exec.getCycleId());
                }
            }
            case DEFECT -> defectService.get(ownerId);
        }
    }
}
