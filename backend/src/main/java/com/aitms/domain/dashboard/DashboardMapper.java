package com.aitms.domain.dashboard;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardMapper {

    /** @param sinceDays null이면 전체, 값이 있으면 최근 N일 등록분 */
    long countActiveTestCases(@Param("sinceDays") Integer sinceDays);

    long countDefectsCreatedSince(@Param("projectId") Long projectId, @Param("sinceDays") int sinceDays);

    long countMyPendingExecutions(@Param("projectId") Long projectId, @Param("userId") Long userId);

    List<MyExecution> findMyPendingExecutions(@Param("projectId") Long projectId,
                                              @Param("userId") Long userId,
                                              @Param("limit") int limit);
}
