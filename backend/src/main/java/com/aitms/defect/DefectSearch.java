package com.aitms.defect;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** projectId 또는 executionId 중 하나는 필수 */
@Getter
@Setter
public class DefectSearch {
    private Long projectId;
    private Long executionId;
    private DefectStatus status;
    private boolean unresolved;   // true면 NEW/OPEN/IN_PROGRESS
    private Severity severity;
    private Long assigneeId;
    private String keyword;       // 코드/제목
    private int page = 1;
    private int size = 20;

    public int getOffset() {
        return (Math.max(page, 1) - 1) * size;
    }

    public List<DefectStatus> getUnresolvedStatuses() {
        return DefectStatus.UNRESOLVED;
    }
}
