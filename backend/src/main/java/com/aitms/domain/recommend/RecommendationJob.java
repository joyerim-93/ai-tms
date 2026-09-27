package com.aitms.domain.recommend;

import java.time.LocalDateTime;

import com.aitms.domain.requirement.RecommendResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/** AI 추천 실행 잡. result 는 조회(GET) 시 SUCCEEDED 잡에만 채워 내려줌. */
@Getter
@Setter
public class RecommendationJob {
    private Long id;
    private Long requirementId;
    private Long atomicRequirementId;   // null = 요구사항 전체
    private JobStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;
    @JsonIgnore
    private String resultJson;

    private RecommendResponse result;
}
