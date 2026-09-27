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

    /** 같은 부모 아래 자기 자신(excludeId)을 뺀 같은 이름 폴더 수 (이름 변경 시 중복 방지) */
    int countSiblingExcluding(@Param("projectId") Long projectId,
                              @Param("parentFolderId") Long parentFolderId,
                              @Param("name") String name,
                              @Param("excludeId") Long excludeId);

    /** 같은 부모 아래 이름이 같은 폴더 id (없으면 empty) */
    Optional<Long> findChildId(@Param("projectId") Long projectId,
                               @Param("parentFolderId") Long parentFolderId,
                               @Param("name") String name);

    void insert(TestCaseFolder folder);

    int updateName(@Param("id") Long id, @Param("name") String name);

    /** 삭제되는 폴더의 직속 하위 폴더를 targetParentId(조부모, 최상위면 NULL)로 승격 */
    int reparentChildFolders(@Param("folderId") Long folderId, @Param("targetParentId") Long targetParentId);

    /** 삭제되는 폴더에 직속된 TC를 targetParentId(최상위면 NULL=미분류)로 이동 */
    int reparentTestCases(@Param("folderId") Long folderId, @Param("targetParentId") Long targetParentId);

    int delete(Long id);

    /** @param unfiledOnly true면 폴더 미지정 TC만 */
    int countTestCases(@Param("projectId") Long projectId, @Param("unfiledOnly") boolean unfiledOnly);
}
