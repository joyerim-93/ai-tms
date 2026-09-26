package com.aitms.domain.defect;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** 코멘트 겸 상태 변경 이력 (statusFrom/To가 있으면 상태 변경) */
@Getter
@Setter
public class DefectComment {
    private Long id;
    private Long defectId;
    private Long authorId;
    private String authorName;
    private String content;
    private DefectStatus statusFrom;
    private DefectStatus statusTo;
    private LocalDateTime createdAt;
}
