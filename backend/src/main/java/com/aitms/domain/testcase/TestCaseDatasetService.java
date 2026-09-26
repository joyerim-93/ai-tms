package com.aitms.domain.testcase;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestCaseDatasetService {

    /** 단계 텍스트의 {변수}와 맞춰야 하므로 공백·기호 없는 이름만 */
    private static final Pattern VARIABLE_NAME = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*");

    private final TestCaseDatasetMapper mapper;
    private final TestCaseMapper testCaseMapper;
    private final ObjectMapper objectMapper;

    public List<TestCaseDataset> list(Long testCaseId) {
        assertTestCase(testCaseId);
        return mapper.findByTestCase(testCaseId);
    }

    @Transactional
    public TestCaseDataset create(Long testCaseId, DatasetRequest req) {
        assertTestCase(testCaseId);
        TestCaseDataset row = apply(new TestCaseDataset(), req);
        row.setTestCaseId(testCaseId);
        mapper.insert(row);
        return get(testCaseId, row.getId());
    }

    @Transactional
    public TestCaseDataset update(Long testCaseId, Long id, DatasetRequest req) {
        TestCaseDataset row = apply(get(testCaseId, id), req);
        mapper.update(row);
        return get(testCaseId, id);
    }

    /** 차수에 실행 항목이 생성된 행은 결과 보존을 위해 삭제 불가 */
    @Transactional
    public void delete(Long testCaseId, Long id) {
        get(testCaseId, id);
        if (mapper.countExecutions(id) > 0) {
            throw ApiException.conflict("테스트 차수에서 실행된 데이터 행은 삭제할 수 없습니다.");
        }
        mapper.delete(id);
    }

    private TestCaseDataset get(Long testCaseId, Long id) {
        TestCaseDataset row = mapper.findById(id)
                .orElseThrow(() -> ApiException.notFound("데이터셋 행을 찾을 수 없습니다. id=" + id));
        if (!row.getTestCaseId().equals(testCaseId)) {
            throw ApiException.notFound("해당 테스트케이스의 데이터셋 행이 아닙니다.");
        }
        return row;
    }

    private void assertTestCase(Long testCaseId) {
        testCaseMapper.findById(testCaseId)
                .orElseThrow(() -> ApiException.notFound("테스트케이스를 찾을 수 없습니다. id=" + testCaseId));
    }

    private TestCaseDataset apply(TestCaseDataset row, DatasetRequest req) {
        for (Map.Entry<String, Object> e : req.paramValues().entrySet()) {
            if (!VARIABLE_NAME.matcher(e.getKey()).matches()) {
                throw badRequest("변수명은 문자·숫자·_ 만 사용할 수 있습니다: " + e.getKey());
            }
            if (e.getValue() instanceof Map || e.getValue() instanceof List) {
                throw badRequest("변수 값은 문자열/숫자/참거짓만 가능합니다: " + e.getKey());
            }
        }
        row.setRowLabel(req.rowLabel().strip());
        row.setParamValues(toJson(req.paramValues()));
        row.setExpectedResultOverride(
                req.expectedResultOverride() == null || req.expectedResultOverride().isBlank()
                        ? null : req.expectedResultOverride().strip());
        return row;
    }

    private String toJson(Map<String, Object> values) {
        try {
            String json = objectMapper.writeValueAsString(values);
            if (json.length() > 2000) {
                throw badRequest("변수 값이 너무 깁니다 (JSON 2000자 이하).");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw badRequest("변수 값을 JSON으로 변환할 수 없습니다.");
        }
    }

    private static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
