package com.aitms.domain.execution;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 테스트수행관리 요청 DTO 모음 */
public final class ExecutionRequests {

    private ExecutionRequests() {
    }

    /** 등록 시 projectId 필수, 수정 시 status 필수 (service에서 검증) */
    public record CycleRequest(
            Long projectId,
            @NotBlank @Size(max = 100) String name,
            LocalDate startDate,
            LocalDate endDate,
            CycleStatus status) {
    }

    public record AddExecutionsRequest(@NotEmpty List<Long> testCaseIds, Long assigneeId) {
    }

    /** assigneeId null = 담당자 해제 */
    public record AssignRequest(@NotEmpty List<Long> executionIds, Long assigneeId) {
    }

    public record ResultRequest(@NotNull ExecutionResult result, @Size(max = 2000) String comment) {
    }
}
