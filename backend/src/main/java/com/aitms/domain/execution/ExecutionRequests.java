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

    /**
     * 자동저장(PATCH) — result/comment 모두 선택. result 생략 시 기존 확정 결과를 유지, comment 생략(null)이면 빈 값으로 저장.
     * result가 실제로 바뀔 때만 이력(test_execution_history)에 한 줄 남고, 코멘트만 바뀌면 현재 값만 갱신(이력 스팸 방지).
     */
    public record PatchRequest(ExecutionResult result, @Size(max = 2000) String comment) {
    }
}
