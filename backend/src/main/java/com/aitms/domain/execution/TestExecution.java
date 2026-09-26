package com.aitms.domain.execution;

import java.time.LocalDateTime;

import com.aitms.common.Priority;
import com.fasterxml.jackson.annotation.JsonRawValue;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestExecution {
    private Long id;
    private Long cycleId;
    private Long testCaseId;
    private Long datasetId;            // 파라미터화 TC의 데이터셋 행 (NULL = TC 단위)
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
    private Boolean tcParameterized;
    private String datasetLabel;
    @JsonRawValue
    private String datasetParams;      // {"amount": 9999} — {변수} 치환은 프론트
    private String datasetExpected;    // {expected} 치환값
    private Integer datasetOrder;
    private String lastComment;        // 최근 결과 입력 코멘트
}
