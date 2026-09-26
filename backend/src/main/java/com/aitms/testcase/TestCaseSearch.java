package com.aitms.testcase;

import com.aitms.common.Priority;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCaseSearch {
    private String keyword;        // 코드/제목/태그
    private String module;
    private Priority priority;
    private TestCaseStatus status;
    private int page = 1;
    private int size = 20;

    public int getOffset() {
        return (Math.max(page, 1) - 1) * size;
    }
}
