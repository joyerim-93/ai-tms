package com.aitms.domain.testcase;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** AI 추천 검수함 — 여러 TC를 한 번에 승인/반려 */
public record BatchReviewRequest(@NotEmpty List<Long> testCaseIds, @NotNull ReviewStatus reviewStatus) {
}
