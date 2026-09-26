package com.aitms.domain.project;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 프로젝트 등록 (테스트케이스 화면에서만 호출) */
public record ProjectRequest(
        @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "영문/숫자/-/_ 만 사용할 수 있습니다.") String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        LocalDate startDate,
        LocalDate endDate) {
}
