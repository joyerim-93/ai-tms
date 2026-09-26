package com.aitms.domain.testcase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** parentFolderId 없으면 프로젝트 최상위 폴더 */
public record FolderRequest(@NotBlank @Size(max = 200) String name, Long parentFolderId) {
}
