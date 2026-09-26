package com.aitms.domain.testcase;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aitms.domain.requirement.AtomicRequirementRef;

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

    // ── 요구사항 다대다 링크
    List<AtomicRequirementRef> findRequirements(Long testCaseId);

    void deleteRequirementLinks(Long testCaseId);

    void insertRequirementLinks(@Param("testCaseId") Long testCaseId, @Param("atomicIds") List<Long> atomicIds);

    /** 주어진 원자 요구사항 중 해당 프로젝트 소속 개수 (교차 프로젝트 링크 방지) */
    int countAtomicsInProject(@Param("atomicIds") List<Long> atomicIds, @Param("projectId") Long projectId);
}
