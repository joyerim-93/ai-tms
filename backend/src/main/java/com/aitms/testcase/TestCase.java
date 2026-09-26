package com.aitms.testcase;

import java.time.LocalDateTime;
import java.util.List;

import com.aitms.common.Priority;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCase {
    private Long id;
    private String tcCode;
    private String title;
    private String module;
    private String precondition;
    private Priority priority;
    private TestCaseStatus status;
    private String tags;
    private Long authorId;
    private String authorName;     // NULL이면 시스템/AI 생성
    private Integer version;
    private Integer stepCount;     // 목록 조회용
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TestStep> steps;  // 상세 조회용

    // 추천/검토
    private TcSource source;
    private TestTechnique technique;
    private ReviewStatus reviewStatus;
    private Long atomicRequirementId;
    private String atomicText;         // 조인
    private Long requirementId;        // 조인 (원자 요구사항의 원문 요구사항)
    private String reqCode;            // 조인
    private Long originProjectId;
    private String originProjectName;  // 조인
    private Long reviewedBy;
    private String reviewedByName;     // 조인
    private LocalDateTime reviewedAt;
}
