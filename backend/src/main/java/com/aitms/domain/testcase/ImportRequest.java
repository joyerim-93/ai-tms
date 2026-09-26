package com.aitms.domain.testcase;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 다른 프로젝트에서 가져오기 (복제).
 *
 * @param projectId   가져올 대상(현재) 프로젝트
 * @param testCaseIds 원본 TC id (다른 프로젝트의 APPROVED·ACTIVE TC)
 * @param folderId    넣을 폴더, null이면 미분류
 */
public record ImportRequest(@NotNull Long projectId, @NotEmpty List<Long> testCaseIds, Long folderId) {
}
