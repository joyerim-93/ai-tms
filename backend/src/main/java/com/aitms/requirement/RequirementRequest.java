package com.aitms.requirement;

import com.aitms.common.Priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 요구사항 등록 (source는 PMS 연동 전까지 MANUAL 고정) */
public record RequirementRequest(
        @NotNull Long projectId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        Priority priority) {
}
