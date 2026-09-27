package com.aitms.domain.recommend;

import java.util.List;

import com.aitms.domain.requirement.AtomicRequirement;

/** 원자 요구사항과 비슷한 과거 TC 검색. 지금은 키워드 유사도, 임베딩 도입 시 구현체만 교체. */
public interface TcRetriever {

    /**
     * @param excludeProjectId 검색에서 제외할 프로젝트(현재 프로젝트)
     * @param limit            최대 결과 수 (점수 내림차순)
     */
    List<Hit> search(AtomicRequirement atomic, Long excludeProjectId, int limit);

    /** @param score 0~1 유사도 */
    record Hit(Long testCaseId, Long projectId, double score) {
    }
}
