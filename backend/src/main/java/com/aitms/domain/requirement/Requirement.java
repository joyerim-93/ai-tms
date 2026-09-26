package com.aitms.domain.requirement;

import java.time.LocalDateTime;
import java.util.List;

import com.aitms.common.Priority;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Requirement {
    private Long id;
    private Long projectId;
    private String reqCode;
    private String title;
    private String description;        // 원문(raw text)
    private RequirementSource source;
    private Priority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private int atomicCount;           // 목록 조회용
    private int testCaseCount;         // 원자 요구사항을 커버하는 TC 수 (중복 제거)
    private int coveredAtomicCount;    // TC가 1건 이상 연결된 원자 요구사항 수
    private List<AtomicRequirement> atomics;  // 상세 조회용
}
