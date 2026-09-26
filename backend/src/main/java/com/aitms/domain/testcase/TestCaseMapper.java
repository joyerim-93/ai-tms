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

    /** @param projectId null이면 전체 */
    List<String> findModules(@Param("projectId") Long projectId);

    void insert(TestCase testCase);

    int update(TestCase testCase);

    int updateReview(@Param("id") Long id,
                     @Param("reviewStatus") ReviewStatus reviewStatus,
                     @Param("reviewedBy") Long reviewedBy);

    int delete(Long id);

    void deleteSteps(Long testCaseId);

    void insertSteps(@Param("steps") List<TestStep> steps);

    int countExecutions(Long testCaseId);

    List<TestCaseRun> findRuns(Long testCaseId);

    /** 같은 프로젝트·원자 요구사항·제목·출처의 ACTIVE TC 수 (추천 재실행 시 중복 생성 방지) */
    int countSameRecommendation(@Param("projectId") Long projectId,
                                @Param("atomicRequirementId") Long atomicRequirementId,
                                @Param("title") String title,
                                @Param("source") TcSource source);

    // ── 요구사항 다대다 링크
    List<AtomicRequirementRef> findRequirements(Long testCaseId);

    void deleteRequirementLinks(Long testCaseId);

    void insertRequirementLinks(@Param("testCaseId") Long testCaseId, @Param("atomicIds") List<Long> atomicIds);

    /** 주어진 원자 요구사항 중 해당 프로젝트 소속 개수 (교차 프로젝트 링크 방지) */
    int countAtomicsInProject(@Param("atomicIds") List<Long> atomicIds, @Param("projectId") Long projectId);
}
