package com.aitms.execution;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestExecutionMapper {

    List<TestExecution> findByCycle(@Param("cycleId") Long cycleId, @Param("search") ExecutionSearch search);

    Optional<TestExecution> findById(Long id);

    /** ACTIVE 이고 아직 차수에 없는 TC만 등록, 등록 건수 반환 */
    int insertAll(@Param("cycleId") Long cycleId,
                  @Param("testCaseIds") List<Long> testCaseIds,
                  @Param("assigneeId") Long assigneeId);

    int updateAssignee(@Param("cycleId") Long cycleId,
                       @Param("executionIds") List<Long> executionIds,
                       @Param("assigneeId") Long assigneeId);

    int updateResult(@Param("id") Long id,
                     @Param("result") ExecutionResult result,
                     @Param("executedBy") Long executedBy);

    int delete(Long id);

    void insertHistory(ExecutionHistory history);

    List<ExecutionHistory> findHistory(Long executionId);

    int countHistory(Long executionId);
}
