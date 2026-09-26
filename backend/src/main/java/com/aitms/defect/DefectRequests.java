package com.aitms.defect;

import com.aitms.common.Priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DefectRequests {

    private DefectRequests() {
    }

    /** 등록 시 projectId 필수. 수정 시 projectId·executionId는 무시(변경 불가). */
    public record DefectRequest(
            Long projectId,
            @NotBlank @Size(max = 200) String title,
            String description,
            @NotNull Severity severity,
            @NotNull Priority priority,
            Long assigneeId,
            Long executionId) {
    }

    public record StatusChangeRequest(@NotNull DefectStatus status, @Size(max = 4000) String comment) {
    }

    public record CommentRequest(@NotBlank @Size(max = 4000) String content) {
    }
}
