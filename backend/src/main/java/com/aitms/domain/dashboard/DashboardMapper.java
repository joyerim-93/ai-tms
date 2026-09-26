package com.aitms.domain.dashboard;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardMapper {

    long countMyPendingExecutions(@Param("projectId") Long projectId, @Param("userId") Long userId);

    List<MyExecution> findMyPendingExecutions(@Param("projectId") Long projectId,
                                              @Param("userId") Long userId,
                                              @Param("limit") int limit);
}
