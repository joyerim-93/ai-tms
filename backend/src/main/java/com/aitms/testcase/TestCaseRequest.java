package com.aitms.testcase;

import java.util.List;

import com.aitms.common.Priority;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 등록/수정 요청. steps 순서가 곧 step_no. */
public record TestCaseRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 100) String module,
        @Size(max = 2000) String precondition,
        @NotNull Priority priority,
        TestCaseStatus status,
        @Size(max = 500) String tags,
        @Valid List<StepRequest> steps) {

    public record StepRequest(
            @NotBlank @Size(max = 2000) String action,
            @Size(max = 2000) String expectedResult) {
    }
}
