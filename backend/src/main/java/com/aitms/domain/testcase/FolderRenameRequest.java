package com.aitms.domain.testcase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 폴더 이름 변경 */
public record FolderRenameRequest(@NotBlank @Size(max = 200) String name) {
}
