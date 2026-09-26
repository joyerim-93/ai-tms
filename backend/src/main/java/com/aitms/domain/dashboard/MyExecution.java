package com.aitms.domain.dashboard;

import com.aitms.common.Priority;

import lombok.Getter;
import lombok.Setter;

/** 내 담당 미수행 항목 (대시보드용) */
@Getter
@Setter
public class MyExecution {
    private Long id;
    private Long cycleId;
    private Integer cycleNo;
    private String cycleName;
    private String tcCode;
    private String tcTitle;
    private Priority tcPriority;
}
