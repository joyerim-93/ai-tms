package com.aitms.domain.recommend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommendation-jobs")
@RequiredArgsConstructor
public class RecommendationJobController {

    private final RecommendationJobService service;

    /** 잡 상태 조회 — SUCCEEDED 이면 result(생성된 DRAFT TC·건너뜀·경고·유사도) 포함 */
    @GetMapping("/{jobId}")
    public RecommendationJob get(@PathVariable Long jobId) {
        return service.get(jobId);
    }

    /** 요구사항의 최근 잡 (없으면 204) */
    @GetMapping("/latest")
    public ResponseEntity<RecommendationJob> latest(@RequestParam Long requirementId) {
        return service.latest(requirementId).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }
}
