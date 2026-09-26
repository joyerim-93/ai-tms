package com.aitms.defect;

import java.util.List;

/**
 * 결함 상태 흐름:
 * NEW → OPEN → IN_PROGRESS → RESOLVED → CLOSED
 * 반려: NEW/OPEN → REJECTED, 재오픈: RESOLVED/CLOSED/REJECTED → OPEN
 */
public enum DefectStatus {
    NEW, OPEN, IN_PROGRESS, RESOLVED, CLOSED, REJECTED;

    public static final List<DefectStatus> UNRESOLVED = List.of(NEW, OPEN, IN_PROGRESS);

    public List<DefectStatus> next() {
        return switch (this) {
            case NEW -> List.of(OPEN, REJECTED);
            case OPEN -> List.of(IN_PROGRESS, REJECTED);
            case IN_PROGRESS -> List.of(RESOLVED);
            case RESOLVED -> List.of(CLOSED, OPEN);
            case CLOSED, REJECTED -> List.of(OPEN);
        };
    }

    public boolean canMoveTo(DefectStatus target) {
        return next().contains(target);
    }
}
