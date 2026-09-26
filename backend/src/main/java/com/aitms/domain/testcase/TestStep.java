package com.aitms.domain.testcase;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestStep {
    private Long id;
    private Long testCaseId;
    private Integer stepNo;
    private String action;
    private String expectedResult;
}
