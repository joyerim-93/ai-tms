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

import com.aitms.domain.recommend.TcRecommendation;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService service;

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

    @PostMapping("/{id}/recommend")
    public List<TcRecommendation> recommend(@PathVariable Long id) {
        return service.recommend(id);
    }
}
