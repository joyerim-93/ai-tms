package com.aitms.domain.defect;

import java.time.LocalDateTime;
import java.util.List;

import com.aitms.common.Priority;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Defect {
    private Long id;
    private Long projectId;
    private String defectCode;
    private String title;
    private String description;
    private Severity severity;
    private Priority priority;
    private DefectStatus status;
    private Long reporterId;
    private Long assigneeId;
    private Long executionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 조인 컬럼
    private String reporterName;
    private String assigneeName;
    private Long testCaseId;
    private String tcCode;
    private String tcTitle;
    private Long cycleId;
    private Integer cycleNo;
    private String cycleName;

    /** 화면의 상태 전환 버튼용 */
    public List<DefectStatus> getNextStatuses() {
        return status == null ? List.of() : status.next();
    }
}
