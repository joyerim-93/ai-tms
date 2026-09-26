package com.aitms.domain.testcase;

import java.util.List;

import com.aitms.common.Priority;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 등록/수정 요청. steps 순서가 곧 step_no.
 * projectId는 등록 시 필수(수정 시 무시 — 프로젝트 이동 불가), folderId null = 미분류.
 * 화면에서 직접 등록한 TC는 source=MANUAL, reviewStatus=APPROVED 로 저장 (출처·검토 상태는 수정 불가).
 */
public record TestCaseRequest(
        Long projectId,
        Long folderId,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 100) String module,
        @Size(max = 2000) String precondition,
        @NotNull Priority priority,
        TestCaseStatus status,
        @Size(max = 500) String tags,
        TestTechnique technique,
        Long atomicRequirementId,
        @Valid List<StepRequest> steps) {

    public record StepRequest(
            @NotBlank @Size(max = 2000) String action,
            @Size(max = 2000) String expectedResult) {
    }

    public record ReviewRequest(@NotNull ReviewStatus reviewStatus) {
    }
}
