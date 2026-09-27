package com.aitms.domain.testcase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestCaseFolderService {

    private final TestCaseFolderMapper mapper;

    /** 평면 목록을 parent_folder_id 기준으로 트리 조립 + 하위 포함 TC 수 계산 */
    public FolderTree tree(Long projectId) {
        List<TestCaseFolder> flat = mapper.findByProject(projectId);
        Map<Long, TestCaseFolder> byId = new LinkedHashMap<>();
        flat.forEach(f -> byId.put(f.getId(), f));

        List<TestCaseFolder> roots = new ArrayList<>();
        for (TestCaseFolder f : flat) {
            TestCaseFolder parent = f.getParentFolderId() == null ? null : byId.get(f.getParentFolderId());
            if (parent == null) {
                roots.add(f);
            } else {
                parent.getChildren().add(f);
            }
        }
        roots.forEach(TestCaseFolderService::sumTotals);
        return new FolderTree(roots, mapper.countTestCases(projectId, false), mapper.countTestCases(projectId, true));
    }

    public TestCaseFolder get(Long id) {
        return mapper.findById(id).orElseThrow(() -> ApiException.notFound("폴더를 찾을 수 없습니다. id=" + id));
    }

    /** 폴더 자신 + 모든 하위 폴더 id (폴더 선택 시 하위 폴더 TC까지 조회) */
    public List<Long> selfAndDescendantIds(Long folderId) {
        TestCaseFolder folder = get(folderId);
        List<TestCaseFolder> flat = mapper.findByProject(folder.getProjectId());
        List<Long> result = new ArrayList<>(List.of(folderId));
        for (int i = 0; i < result.size(); i++) {
            Long parentId = result.get(i);
            flat.stream().filter(f -> parentId.equals(f.getParentFolderId())).forEach(f -> result.add(f.getId()));
        }
        return result;
    }

    /** 폴더가 해당 프로젝트 소속인지 검증 (null 허용 = 미분류) */
    public void assertInProject(Long folderId, Long projectId) {
        if (folderId != null && !get(folderId).getProjectId().equals(projectId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "다른 프로젝트의 폴더입니다.");
        }
    }

    @Transactional
    public TestCaseFolder create(Long projectId, FolderRequest req) {
        assertInProject(req.parentFolderId(), projectId);
        String name = req.name().strip();
        if (mapper.countSibling(projectId, req.parentFolderId(), name) > 0) {
            throw ApiException.conflict("같은 위치에 '" + name + "' 폴더가 이미 있습니다.");
        }
        TestCaseFolder folder = new TestCaseFolder();
        folder.setProjectId(projectId);
        folder.setParentFolderId(req.parentFolderId());
        folder.setName(name);
        mapper.insert(folder);
        return get(folder.getId());
    }

    /** 경로(상위→하위 이름 목록)의 마지막 폴더 id — 없는 폴더는 순서대로 만들어 가며 내려감 (엑셀 업로드) */
    @Transactional
    public Long findOrCreatePath(Long projectId, List<String> names) {
        Long parentId = null;
        for (String name : names) {
            Long existing = mapper.findChildId(projectId, parentId, name).orElse(null);
            if (existing != null) {
                parentId = existing;
                continue;
            }
            TestCaseFolder folder = new TestCaseFolder();
            folder.setProjectId(projectId);
            folder.setParentFolderId(parentId);
            folder.setName(name);
            mapper.insert(folder);
            parentId = folder.getId();
        }
        return parentId;
    }

    /** 이름 변경 — 같은 위치에 같은 이름이 있으면 409 (자기 자신 제외) */
    @Transactional
    public TestCaseFolder rename(Long projectId, Long id, FolderRenameRequest req) {
        TestCaseFolder folder = get(id);
        if (!folder.getProjectId().equals(projectId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "다른 프로젝트의 폴더입니다.");
        }
        String name = req.name().strip();
        if (mapper.countSiblingExcluding(projectId, folder.getParentFolderId(), name, id) > 0) {
            throw ApiException.conflict("같은 위치에 '" + name + "' 폴더가 이미 있습니다.");
        }
        mapper.updateName(id, name);
        return get(id);
    }

    /**
     * 폴더 삭제 — 안의 TC·직속 하위 폴더는 삭제되는 폴더의 부모(없으면 프로젝트 최상위/미분류)로 승격시킨 뒤 삭제.
     * (하위 폴더 안에 더 중첩된 내용은 그 하위 폴더를 따라 그대로 이동 — 이 폴더의 '직속'만 한 단계 올라감)
     */
    @Transactional
    public void delete(Long projectId, Long id) {
        TestCaseFolder folder = get(id);
        if (!folder.getProjectId().equals(projectId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "다른 프로젝트의 폴더입니다.");
        }
        Long targetParentId = folder.getParentFolderId();
        mapper.reparentChildFolders(id, targetParentId);
        mapper.reparentTestCases(id, targetParentId);
        mapper.delete(id);
    }

    private static int sumTotals(TestCaseFolder folder) {
        int total = folder.getTestCaseCount();
        for (TestCaseFolder child : folder.getChildren()) {
            total += sumTotals(child);
        }
        folder.setTotalCount(total);
        return total;
    }
}
