package com.aitms.testcase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.aitms.common.CurrentUser;
import com.aitms.common.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestCaseService {

    private final TestCaseMapper mapper;

    public PageResponse<TestCase> search(TestCaseSearch search) {
        return new PageResponse<>(mapper.search(search), mapper.count(search), search.getPage(), search.getSize());
    }

    public TestCase get(Long id) {
        TestCase tc = mapper.findById(id)
                .orElseThrow(() -> ApiException.notFound("테스트케이스를 찾을 수 없습니다. id=" + id));
        tc.setSteps(mapper.findSteps(id));
        return tc;
    }

    public List<String> modules() {
        return mapper.findModules();
    }

    @Transactional
    public TestCase create(TestCaseRequest req) {
        TestCase tc = apply(new TestCase(), req);
        tc.setAuthorId(CurrentUser.id());
        mapper.insert(tc);
        saveSteps(tc.getId(), req.steps());
        return get(tc.getId());
    }

    /** 수정할 때마다 version +1, 단계는 전체 교체. */
    @Transactional
    public TestCase update(Long id, TestCaseRequest req) {
        TestCase tc = apply(get(id), req);
        mapper.update(tc);
        mapper.deleteSteps(id);
        saveSteps(id, req.steps());
        return get(id);
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
        tc.setTitle(req.title().strip());
        tc.setModule(blankToNull(req.module()));
        tc.setPrecondition(blankToNull(req.precondition()));
        tc.setPriority(req.priority());
        tc.setStatus(req.status() != null ? req.status() : TestCaseStatus.ACTIVE);
        tc.setTags(blankToNull(req.tags()));
        return tc;
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
