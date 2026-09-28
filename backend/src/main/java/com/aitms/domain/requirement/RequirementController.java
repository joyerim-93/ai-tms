package com.aitms.domain.requirement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import com.aitms.domain.recommend.RecommendationJob;
import com.aitms.domain.recommend.RecommendationJobService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService service;
    private final RecommendationJobService jobService;

    @GetMapping
    public List<Requirement> list(@RequestParam Long projectId) {
        return service.findByProject(projectId);
    }

    /** 프로젝트의 원자 요구사항 전체 — TC 폼 '검증하는 요구사항' 선택용 */
    @GetMapping("/atomics")
    public List<AtomicRequirementRef> atomics(@RequestParam Long projectId) {
        return service.atomicRefs(projectId);
    }

    @GetMapping("/{id}")
    public Requirement get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Requirement create(@Validated @RequestBody RequirementRequest req) {
        return service.create(req);
    }

    /** AI 추천 요청 — 잡(RUNNING)을 만들어 즉시 반환(202). 진행/결과는 GET /api/recommendation-jobs/{id} 로 폴링. */
    @PostMapping("/{id}/recommend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RecommendationJob recommend(@PathVariable Long id) {
        return jobService.start(id);
    }

    /**
     * 원자 요구사항 자동 분해(LLM, 동기) — 원문 하나에 LLM 호출 1회라 잡 없이 바로 처리.
     * 이미 원자 요구사항이 있으면 그대로 반환(재호출 안 함).
     */
    @PostMapping("/{id}/decompose")
    public Requirement decompose(@PathVariable Long id) {
        return service.decompose(id);
    }
}
