package com.aitms.domain.testcase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** 테스트케이스 폴더 (프로젝트별 트리). children/totalCount는 서비스에서 조립 */
@Getter
@Setter
public class TestCaseFolder {
    private Long id;
    private Long projectId;
    private Long parentFolderId;   // NULL = 최상위
    private String name;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    private int testCaseCount;     // 이 폴더에 직접 속한 TC 수 (조회 시 집계)
    private int totalCount;        // 하위 폴더 포함 TC 수
    private List<TestCaseFolder> children = new ArrayList<>();
}
