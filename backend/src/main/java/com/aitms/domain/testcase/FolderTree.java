package com.aitms.domain.testcase;

import java.util.List;

/**
 * 프로젝트 폴더 트리 응답.
 *
 * @param roots        최상위 폴더들 (children 재귀)
 * @param totalCount   프로젝트 전체 TC 수
 * @param unfiledCount 폴더 미지정(미분류) TC 수
 */
public record FolderTree(List<TestCaseFolder> roots, int totalCount, int unfiledCount) {
}
