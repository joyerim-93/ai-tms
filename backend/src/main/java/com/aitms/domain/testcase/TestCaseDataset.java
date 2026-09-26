package com.aitms.domain.testcase;

import com.fasterxml.jackson.annotation.JsonRawValue;

import lombok.Getter;
import lombok.Setter;

/**
 * 파라미터화 TC 데이터셋 행.
 * paramValues는 DB의 JSON 문자열을 그대로 객체로 내려줌({"amount": 9999}) — {변수} 치환은 프론트에서.
 */
@Getter
@Setter
public class TestCaseDataset {
    private Long id;
    private Long testCaseId;
    private String rowLabel;
    @JsonRawValue
    private String paramValues;
    private String expectedResultOverride;  // {expected} 치환값
    private Integer sortOrder;
}
