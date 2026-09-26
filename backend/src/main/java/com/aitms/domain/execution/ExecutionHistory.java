package com.aitms.domain.execution;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionHistory {
    private Long id;
    private Long executionId;
    private ExecutionResult result;
    private String comment;
    private Long executedBy;
    private String executedByName;
    private LocalDateTime executedAt;
}
