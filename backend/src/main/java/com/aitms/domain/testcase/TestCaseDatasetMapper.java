package com.aitms.domain.testcase;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestCaseDatasetMapper {

    List<TestCaseDataset> findByTestCase(Long testCaseId);

    Optional<TestCaseDataset> findById(Long id);

    /** sortOrder가 null이면 같은 TC의 마지막 순서로 */
    void insert(TestCaseDataset dataset);

    int update(TestCaseDataset dataset);

    int delete(Long id);

    /** 이 행으로 생성된 차수 실행 항목 수 (있으면 삭제 불가) */
    int countExecutions(Long datasetId);
}
