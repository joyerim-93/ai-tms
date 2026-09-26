package com.aitms.domain.testcase;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 데이터셋 행 등록/수정.
 *
 * @param paramValues 변수명 → 값 (문자열/숫자/불리언). 변수명은 문자·숫자·_ (첫 글자 숫자 불가)
 */
public record DatasetRequest(
        @NotBlank @Size(max = 200) String rowLabel,
        @NotNull Map<String, Object> paramValues,
        @Size(max = 2000) String expectedResultOverride) {
}
