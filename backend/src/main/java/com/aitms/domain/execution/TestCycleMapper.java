package com.aitms.domain.execution;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestCycleMapper {

    List<TestCycle> findByProject(Long projectId);

    Optional<TestCycle> findById(Long id);

    void insert(TestCycle cycle);

    int update(TestCycle cycle);

    int updateStatus(@Param("id") Long id, @Param("status") CycleStatus status);

    int delete(Long id);

    int countHistory(Long cycleId);
}
