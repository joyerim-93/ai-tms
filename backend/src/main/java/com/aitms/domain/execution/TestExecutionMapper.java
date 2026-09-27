package com.aitms.domain.execution;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestExecutionMapper {

    List<TestExecution> findByCycle(@Param("cycleId") Long cycleId, @Param("search") ExecutionSearch search);

    /** 엑셀 '전체 차수 다운로드' — 프로젝트의 모든 차수, 차수 번호·TC 순 */
    List<TestExecution> findByProject(Long projectId);

    Optional<TestExecution> findById(Long id);

    /** 차수와 같은 프로젝트의 ACTIVE·APPROVED 이고 아직 차수에 없는 일반 TC 등록 (TC 단위 1행), 등록 건수 반환 */
    int insertAll(@Param("cycleId") Long cycleId,
                  @Param("testCaseIds") List<Long> testCaseIds,
                  @Param("assigneeId") Long assigneeId);

    /** 파라미터화 TC는 데이터셋 행마다 실행 항목 생성, 생성 건수 반환 */
    int insertDatasetRows(@Param("cycleId") Long cycleId,
                          @Param("testCaseIds") List<Long> testCaseIds,
                          @Param("assigneeId") Long assigneeId);

    int updateAssignee(@Param("cycleId") Long cycleId,
                       @Param("executionIds") List<Long> executionIds,
                       @Param("assigneeId") Long assigneeId);

    int updateResult(@Param("id") Long id,
                     @Param("result") ExecutionResult result,
                     @Param("executedBy") Long executedBy);

    int updateDraft(@Param("id") Long id,
                    @Param("result") ExecutionResult result,
                    @Param("comment") String comment);

    int delete(Long id);

    void insertHistory(ExecutionHistory history);

    List<ExecutionHistory> findHistory(Long executionId);

    int countHistory(Long executionId);
}
