package com.aitms.domain.testcase;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** TC 상세 '실행 이력' 탭 한 줄 — test_execution_history 기준 (결과 입력 1회 = 1행) */
@Getter
@Setter
public class TestCaseRun {
    private Long id;             // history id
    private Long executionId;
    private Long cycleId;
    private Integer cycleNo;
    private String cycleName;
    private Long datasetId;      // 파라미터화 TC면 데이터셋 행
    private String datasetLabel;
    private String result;       // PASS | FAIL | BLOCKED | NOT_RUN
    private String comment;
    private String executedByName;
    private LocalDateTime executedAt;
}
