package com.aitms.execution;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCycle {
    private Long id;
    private Long projectId;
    private Integer cycleNo;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private CycleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 진행 현황 (조회 시 집계)
    private int totalCount;
    private int passCount;
    private int failCount;
    private int blockedCount;
    private int notRunCount;
}
