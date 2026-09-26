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
    private String authorName;
    private Integer version;
    private Integer stepCount;     // 목록 조회용
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TestStep> steps;  // 상세 조회용
}
