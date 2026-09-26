package com.aitms.domain.execution;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionSearch {
    private ExecutionResult result;
    private Long assigneeId;
    private String keyword;  // TC 코드/제목
}
