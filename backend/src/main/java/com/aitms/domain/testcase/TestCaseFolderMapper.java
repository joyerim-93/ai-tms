package com.aitms.domain.testcase;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestCaseFolderMapper {

    /** 프로젝트의 모든 폴더(평면) + 폴더별 직접 TC 수 — 트리는 서비스에서 parent_folder_id로 조립 */
    List<TestCaseFolder> findByProject(Long projectId);

    Optional<TestCaseFolder> findById(Long id);

    /** 같은 부모 아래 같은 이름 폴더 수 (중복 방지) */
    int countSibling(@Param("projectId") Long projectId,
                     @Param("parentFolderId") Long parentFolderId,
                     @Param("name") String name);

    void insert(TestCaseFolder folder);

    /** @param unfiledOnly true면 폴더 미지정 TC만 */
    int countTestCases(@Param("projectId") Long projectId, @Param("unfiledOnly") boolean unfiledOnly);
}
