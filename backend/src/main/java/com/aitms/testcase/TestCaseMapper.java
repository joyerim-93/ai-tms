package com.aitms.testcase;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestCaseMapper {

    List<TestCase> search(TestCaseSearch search);

    long count(TestCaseSearch search);

    Optional<TestCase> findById(Long id);

    List<TestStep> findSteps(Long testCaseId);

    List<String> findModules();

    void insert(TestCase testCase);

    int update(TestCase testCase);

    int updateReview(@Param("id") Long id,
                     @Param("reviewStatus") ReviewStatus reviewStatus,
                     @Param("reviewedBy") Long reviewedBy);

    int delete(Long id);

    void deleteSteps(Long testCaseId);

    void insertSteps(@Param("steps") List<TestStep> steps);

    int countExecutions(Long testCaseId);
}
