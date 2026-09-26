package com.aitms.domain.testcase;

import java.time.LocalDateTime;
import java.util.List;

import com.aitms.common.Priority;
import com.aitms.domain.requirement.AtomicRequirementRef;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCase {
    private Long id;
    private String tcCode;
    private Long projectId;
    private String projectName;    // 조인 (가져오기 검색용)
    private Long folderId;         // NULL = 미분류
    private String folderName;     // 조인
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
    private List<AtomicRequirementRef> requirements; // 상세 조회: 검증하는 요구사항 (다대다)
    private int requirementCount;                    // 목록 조회: 연결 요구사항 수
    private Long originProjectId;
    private String originProjectName;  // 조인
    private Long reviewedBy;
    private String reviewedByName;     // 조인
    private LocalDateTime reviewedAt;
}
