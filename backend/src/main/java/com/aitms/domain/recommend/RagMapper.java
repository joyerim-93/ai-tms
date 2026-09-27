package com.aitms.domain.recommend;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RagMapper {

    /** 검색 대상 TC — excludeProjectId 를 제외한 전체 프로젝트의 APPROVED·ACTIVE (RAG는 project_id로 제한하지 않음) */
    List<RagCandidate> findCandidates(@Param("excludeProjectId") Long excludeProjectId);

    /** 위 후보 TC들의 요구사항 링크 */
    List<RagLink> findLinks(@Param("excludeProjectId") Long excludeProjectId);
}
