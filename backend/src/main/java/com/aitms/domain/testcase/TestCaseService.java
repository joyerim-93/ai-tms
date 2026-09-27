package com.aitms.domain.testcase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.CurrentUser;
import com.aitms.common.PageResponse;
import com.aitms.common.Priority;
import com.aitms.domain.project.ProjectService;
import com.aitms.domain.recommend.TcRecommendation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestCaseService {

    private final TestCaseMapper mapper;
    private final TestCaseFolderService folderService;
    private final TestCaseDatasetMapper datasetMapper;
    private final ObjectMapper objectMapper;
    private final ProjectService projectService;

    /** folderId 지정 시 하위 폴더 TC까지 포함 */
    public PageResponse<TestCase> search(TestCaseSearch search) {
        if (search.getFolderId() != null) {
            search.setFolderIds(folderService.selfAndDescendantIds(search.getFolderId()));
        }
        return new PageResponse<>(mapper.search(search), mapper.count(search), search.getPage(), search.getSize());
    }

    public TestCase get(Long id) {
        TestCase tc = mapper.findById(id)
                .orElseThrow(() -> ApiException.notFound("테스트케이스를 찾을 수 없습니다. id=" + id));
        tc.setSteps(mapper.findSteps(id));
        tc.setRequirements(mapper.findRequirements(id));
        tc.setDatasets(datasetMapper.findByTestCase(id));
        return tc;
    }

    public List<String> modules(Long projectId) {
        return mapper.findModules(projectId);
    }

    @Transactional
    public TestCase create(TestCaseRequest req) {
        if (req.projectId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "projectId: 필수 값입니다.");
        }
        folderService.assertInProject(req.folderId(), req.projectId());
        TestCase tc = apply(new TestCase(), req);
        tc.setProjectId(req.projectId());
        tc.setAuthorId(CurrentUser.id());
        tc.setSource(TcSource.MANUAL);
        tc.setReviewStatus(ReviewStatus.APPROVED);
        mapper.insert(tc);
        saveSteps(tc.getId(), req.steps());
        saveRequirementLinks(tc.getId(), tc.getProjectId(), req.atomicRequirementIds());
        return get(tc.getId());
    }

    /**
     * 엑셀 업로드로 만든 TC — source=MANUAL, 검토 상태 DRAFT(승인 후 차수에 등록 가능), 상태 ACTIVE, 우선순위 MEDIUM.
     * 폴더는 호출 측(TestCaseExcelService)이 프로젝트 소속으로 만든 것만 넘김.
     */
    @Transactional
    public TestCase createUploaded(Long projectId, Long folderId, String title, TestTechnique technique,
                                   List<TestCaseRequest.StepRequest> steps) {
        TestCase tc = new TestCase();
        tc.setProjectId(projectId);
        tc.setFolderId(folderId);
        tc.setTitle(title);
        tc.setPriority(Priority.MEDIUM);
        tc.setStatus(TestCaseStatus.ACTIVE);
        tc.setTechnique(technique);
        tc.setIsParameterized(false);
        tc.setAuthorId(CurrentUser.id());
        tc.setSource(TcSource.MANUAL);
        tc.setReviewStatus(ReviewStatus.DRAFT);
        mapper.insert(tc);
        saveSteps(tc.getId(), steps);
        return tc;
    }

    /** 수정할 때마다 version +1, 단계는 전체 교체(요구사항 연결은 atomicRequirementIds를 보냈을 때만 교체). 프로젝트는 변경 불가(폴더 이동은 같은 프로젝트 안에서만). */
    @Transactional
    public TestCase update(Long id, TestCaseRequest req) {
        TestCase tc = get(id);
        folderService.assertInProject(req.folderId(), tc.getProjectId());
        apply(tc, req);
        mapper.update(tc);
        mapper.deleteSteps(id);
        saveSteps(id, req.steps());
        if (req.atomicRequirementIds() != null) {   // 생략(null)이면 기존 연결 유지 — 폼은 연결을 다루지 않고 상세의 '연결된 요구사항' 탭에서 관리
            mapper.deleteRequirementLinks(id);
            saveRequirementLinks(id, tc.getProjectId(), req.atomicRequirementIds());
        }
        return get(id);
    }

    /**
     * 추천 후보를 DRAFT TC로 저장 — 파라미터화 TC + 데이터셋, 원자 요구사항 링크, 미분류 폴더, 작성자 없음(시스템).
     *
     * @return 저장된 TC, 같은 추천 TC가 이미 있으면 empty
     */
    @Transactional
    public Optional<TestCase> createDraft(TcRecommendation rec, Long projectId) {
        if (mapper.countSameRecommendation(projectId, rec.atomicRequirementId(), rec.title(), rec.source()) > 0) {
            return Optional.empty();
        }
        TestCase tc = new TestCase();
        tc.setProjectId(projectId);
        tc.setTitle(rec.title());
        tc.setPriority(Priority.MEDIUM);
        tc.setStatus(TestCaseStatus.ACTIVE);
        tc.setTechnique(rec.technique());
        tc.setIsParameterized(rec.parameterized());
        tc.setSource(rec.source());
        tc.setReviewStatus(ReviewStatus.DRAFT);
        tc.setOriginProjectId(rec.originProjectId());
        mapper.insert(tc);

        saveSteps(tc.getId(), rec.steps().stream()
                .map(s -> new TestCaseRequest.StepRequest(s.action(), s.expectedResult()))
                .toList());
        saveRequirementLinks(tc.getId(), projectId, List.of(rec.atomicRequirementId()));
        int order = 1;
        for (TcRecommendation.DatasetRow row : rec.datasetRows()) {
            TestCaseDataset d = new TestCaseDataset();
            d.setTestCaseId(tc.getId());
            d.setRowLabel(row.rowLabel());
            d.setParamValues(toJson(row.paramValues()));
            d.setExpectedResultOverride(row.expectedResultOverride());
            d.setSortOrder(order++);
            datasetMapper.insert(d);
        }
        return Optional.of(get(tc.getId()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** '연결된 요구사항' 탭에서 링크만 교체 */
    @Transactional
    public TestCase replaceRequirements(Long id, List<Long> atomicRequirementIds) {
        TestCase tc = get(id);
        mapper.deleteRequirementLinks(id);
        saveRequirementLinks(id, tc.getProjectId(), atomicRequirementIds);
        return get(id);
    }

    /** 이 TC의 실행 이력 (모든 차수, 시간 역순) */
    public List<TestCaseRun> runs(Long id) {
        get(id);
        return mapper.findRuns(id);
    }

    /** AI 추천(DRAFT) TC 승인/반려. 검토자 = CurrentUser */
    @Transactional
    public TestCase review(Long id, ReviewStatus reviewStatus) {
        get(id);
        mapper.updateReview(id, reviewStatus, CurrentUser.id());
        return get(id);
    }

    /**
     * 다른 프로젝트의 승인된 TC를 현재 프로젝트에 새 row로 복제 ('중앙관리'는 공유가 아니라 복제로 구현).
     * origin_project_id = 원본 TC의 프로젝트. 요구사항 링크는 프로젝트별이라 복사하지 않음.
     *
     * @return 복제된 TC 목록 (같은 프로젝트·미승인·폐기 TC는 건너뜀)
     */
    /** '공통 테스트케이스' 마스터 프로젝트는 자체 관리만 — 다른 프로젝트 TC를 이 프로젝트로 복사해올 수 없음 */
    static final String COMMON_PROJECT_CODE = "COMMON-TC";

    @Transactional
    public List<TestCase> importFrom(ImportRequest req) {
        if (COMMON_PROJECT_CODE.equals(projectService.get(req.projectId()).getCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'공통 테스트케이스' 프로젝트로는 다른 프로젝트의 TC를 가져올 수 없습니다.");
        }
        folderService.assertInProject(req.folderId(), req.projectId());
        List<TestCase> imported = new ArrayList<>();
        for (Long sourceId : req.testCaseIds()) {
            TestCase src = get(sourceId);
            if (src.getProjectId().equals(req.projectId())
                    || src.getReviewStatus() != ReviewStatus.APPROVED
                    || src.getStatus() != TestCaseStatus.ACTIVE) {
                continue;
            }
            TestCase copy = new TestCase();
            copy.setProjectId(req.projectId());
            copy.setFolderId(req.folderId());
            copy.setTitle(src.getTitle());
            copy.setModule(src.getModule());
            copy.setPrecondition(src.getPrecondition());
            copy.setPriority(src.getPriority());
            copy.setStatus(TestCaseStatus.ACTIVE);
            copy.setTags(src.getTags());
            copy.setTechnique(src.getTechnique());
            copy.setIsParameterized(src.getIsParameterized());
            copy.setSource(TcSource.MANUAL);
            copy.setReviewStatus(ReviewStatus.APPROVED);
            copy.setOriginProjectId(src.getProjectId());
            copy.setAuthorId(CurrentUser.id());
            mapper.insert(copy);

            List<TestStep> steps = src.getSteps().stream()
                    .map(s -> new TestStep(null, copy.getId(), s.getStepNo(), s.getAction(), s.getExpectedResult()))
                    .toList();
            if (!steps.isEmpty()) {
                mapper.insertSteps(steps);
            }
            src.getDatasets().forEach(d -> {
                d.setId(null);
                d.setTestCaseId(copy.getId());
                datasetMapper.insert(d);
            });
            imported.add(get(copy.getId()));
        }
        return imported;
    }

    /** 차수에 등록된 적 있는 TC는 이력 보존을 위해 삭제 불가 → 폐기(DEPRECATED) 처리 유도. */
    @Transactional
    public void delete(Long id) {
        get(id);
        if (mapper.countExecutions(id) > 0) {
            throw ApiException.conflict("테스트 차수에 등록된 테스트케이스는 삭제할 수 없습니다. 상태를 '폐기'로 변경하세요.");
        }
        mapper.delete(id);
    }

    private TestCase apply(TestCase tc, TestCaseRequest req) {
        tc.setFolderId(req.folderId());
        tc.setTitle(req.title().strip());
        tc.setModule(blankToNull(req.module()));
        tc.setPrecondition(blankToNull(req.precondition()));
        tc.setPriority(req.priority());
        tc.setStatus(req.status() != null ? req.status() : TestCaseStatus.ACTIVE);
        tc.setTags(blankToNull(req.tags()));
        tc.setTechnique(req.technique());
        tc.setIsParameterized(Boolean.TRUE.equals(req.isParameterized()));
        return tc;
    }

    /** 다대다 링크 저장 — 같은 프로젝트의 원자 요구사항만 허용 */
    private void saveRequirementLinks(Long testCaseId, Long projectId, List<Long> atomicIds) {
        if (atomicIds == null || atomicIds.isEmpty()) {
            return;
        }
        List<Long> distinct = atomicIds.stream().distinct().toList();
        if (mapper.countAtomicsInProject(distinct, projectId) != distinct.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "같은 프로젝트의 요구사항만 연결할 수 있습니다.");
        }
        mapper.insertRequirementLinks(testCaseId, distinct);
    }

    private void saveSteps(Long testCaseId, List<TestCaseRequest.StepRequest> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        List<TestStep> rows = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            TestCaseRequest.StepRequest s = steps.get(i);
            rows.add(new TestStep(null, testCaseId, i + 1, s.action().strip(), blankToNull(s.expectedResult())));
        }
        mapper.insertSteps(rows);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
