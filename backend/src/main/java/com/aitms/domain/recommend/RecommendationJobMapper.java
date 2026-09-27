package com.aitms.domain.recommend;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationJobMapper {

    void insert(RecommendationJob job);

    Optional<RecommendationJob> findById(Long id);

    /** 이 요구사항의 진행 중(PENDING/RUNNING) 잡 — 중복 요청 방지 */
    Optional<RecommendationJob> findActive(Long requirementId);

    Optional<RecommendationJob> findLatest(Long requirementId);

    int finish(@Param("id") Long id,
               @Param("status") JobStatus status,
               @Param("errorMessage") String errorMessage,
               @Param("resultJson") String resultJson);

    /** 서버 재시작 등으로 끝나지 못한 잡 정리 */
    int failActive(@Param("errorMessage") String errorMessage);
}
