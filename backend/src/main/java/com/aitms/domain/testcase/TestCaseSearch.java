package com.aitms.domain.testcase;

import java.util.List;

import com.aitms.common.Priority;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCaseSearch {
    private Long projectId;        // 없으면 전체 프로젝트 ('다른 프로젝트에서 가져오기' 검색)
    private Long excludeProjectId; // 가져오기 검색 시 현재 프로젝트 제외
    private Long folderId;         // 하위 폴더 포함 (서비스에서 folderIds로 확장)
    private boolean unfiled;       // true면 폴더 미지정(미분류)만
    private List<Long> folderIds;  // 내부용
    private String keyword;        // 코드/제목/태그
    private String module;
    private Priority priority;
    private TestCaseStatus status;
    private TcSource source;
    private ReviewStatus reviewStatus;
    private Long atomicRequirementId;
    private int page = 1;
    private int size = 20;

    public int getOffset() {
        return (Math.max(page, 1) - 1) * size;
    }
}
