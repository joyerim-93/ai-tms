package com.aitms.execution;

import java.time.LocalDateTime;

import com.aitms.common.Priority;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestExecution {
    private Long id;
    private Long cycleId;
    private Long testCaseId;
    private Integer tcVersion;         // 차수 등록 시점 버전
    private Long assigneeId;
    private ExecutionResult result;
    private Long executedBy;
    private LocalDateTime executedAt;

    // 조인 컬럼
    private String tcCode;
    private String tcTitle;
    private String tcModule;
    private Priority tcPriority;
    private Integer currentTcVersion;  // 저장소의 현재 버전 (tcVersion과 다르면 등록 후 수정된 것)
    private String assigneeName;
    private String executedByName;
}
