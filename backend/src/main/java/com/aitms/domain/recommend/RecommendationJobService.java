package com.aitms.domain.recommend;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.aitms.common.ApiException;
import com.aitms.domain.requirement.RecommendResponse;
import com.aitms.domain.requirement.RequirementService;
import com.aitms.domain.testcase.TestCase;
import com.aitms.domain.testcase.TestCaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AI 추천 잡 — start()가 RUNNING 잡을 만들어 즉시 반환하고 추천은 백그라운드 스레드에서 실행,
 * 끝나면 SUCCEEDED/FAILED 로 갱신. 잡 상태 갱신은 추천 트랜잭션과 분리(추천이 롤백돼도 FAILED 기록이 남음).
 */
@Service
public class RecommendationJobService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationJobService.class);

    private final RecommendationJobMapper mapper;
    private final RequirementService requirementService;
    private final TestCaseService testCaseService;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    public RecommendationJobService(RecommendationJobMapper mapper, RequirementService requirementService,
                                    TestCaseService testCaseService, ObjectMapper objectMapper,
                                    @Qualifier("recommendationExecutor") Executor executor) {
        this.mapper = mapper;
        this.requirementService = requirementService;
        this.testCaseService = testCaseService;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    /** 결과 요약(저장용) — TC 본문은 조회 시 다시 읽음 */
    record JobResult(List<Long> createdIds, int skipped, List<String> warnings, Map<Long, BigDecimal> scores) {
    }

    /** 잡 생성(RUNNING) + 백그라운드 실행. 이미 진행 중인 잡이 있으면 그 잡을 그대로 반환(중복 실행 방지). */
    public RecommendationJob start(Long requirementId) {
        return create(requirementId);
    }

    private RecommendationJob create(Long requirementId) {
        requirementService.get(requirementId); // 없으면 404
        Optional<RecommendationJob> active = mapper.findActive(requirementId);
        if (active.isPresent()) {
            return active.get();
        }
        RecommendationJob job = new RecommendationJob();
        job.setRequirementId(requirementId);
        job.setStatus(JobStatus.RUNNING);
        mapper.insert(job);
        Long id = job.getId();
        executor.execute(() -> run(id));
        return mapper.findById(id).orElseThrow();
    }

    /** 추천 실행 본체 (executor 스레드에서 호출, 테스트에서는 직접 호출 가능) */
    public void run(Long jobId) {
        RecommendationJob job = mapper.findById(jobId).orElseThrow();
        try {
            RecommendResponse res = requirementService.recommend(job.getRequirementId());
            JobResult result = new JobResult(res.created().stream().map(TestCase::getId).toList(),
                    res.skipped(), res.warnings(), res.scores());
            mapper.finish(jobId, JobStatus.SUCCEEDED, null, objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("추천 잡 {} 실패", jobId, e);
            mapper.finish(jobId, JobStatus.FAILED, truncate(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), null);
        }
    }

    public RecommendationJob get(Long jobId) {
        RecommendationJob job = mapper.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("추천 잡을 찾을 수 없습니다. id=" + jobId));
        return withResult(job);
    }

    /** 요구사항의 가장 최근 잡 (없으면 empty) — 화면 재진입 시 진행 중/실패 상태 복원용 */
    public Optional<RecommendationJob> latest(Long requirementId) {
        return mapper.findLatest(requirementId).map(this::withResult);
    }

    private RecommendationJob withResult(RecommendationJob job) {
        if (job.getStatus() == JobStatus.SUCCEEDED && job.getResultJson() != null) {
            try {
                JobResult r = objectMapper.readValue(job.getResultJson(), JobResult.class);
                List<TestCase> created = new ArrayList<>();
                for (Long id : r.createdIds()) {
                    try {
                        created.add(testCaseService.get(id));
                    } catch (ApiException ignored) {
                        // 이후 삭제된 TC
                    }
                }
                job.setResult(new RecommendResponse(created, r.skipped(), r.warnings(), r.scores()));
            } catch (JsonProcessingException e) {
                log.warn("추천 잡 {} 결과 해석 실패", job.getId(), e);
            }
        }
        return job;
    }

    /** 서버가 내려가며 끝나지 못한 잡은 실패 처리 */
    @EventListener(ApplicationReadyEvent.class)
    void failInterruptedJobs() {
        int n = mapper.failActive("서버 재시작으로 추천이 중단되었습니다.");
        if (n > 0) {
            log.info("중단된 추천 잡 {}건을 FAILED 처리", n);
        }
    }

    private static String truncate(String s) {
        return s.length() > 900 ? s.substring(0, 900) : s;
    }
}
