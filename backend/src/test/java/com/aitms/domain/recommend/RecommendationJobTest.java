package com.aitms.domain.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.common.ApiException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/** 잡 상태 전이 — 실행기를 동기(Runnable::run)로 바꿔 한 트랜잭션 안에서 검증 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jobtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.main.allow-bean-definition-overriding=true"})
@Import(RecommendationJobTest.SyncExecutor.class)
@Transactional
class RecommendationJobTest {

    @TestConfiguration
    static class SyncExecutor {
        @Bean("recommendationExecutor")
        Executor recommendationExecutor() {
            return Runnable::run;
        }
    }

    @Autowired RecommendationJobService jobService;
    @Autowired RecommendationJobMapper jobMapper;
    @MockitoBean RecommendationService recommendationService;

    @Test
    void 추천이_끝나면_SUCCEEDED로_기록되고_결과에_생성된_TC가_담긴다() {
        when(recommendationService.recommend(anyLong())).thenAnswer(inv -> new RuleBasedStub().result());
        RecommendationJob started = jobService.start(1L);

        assertThat(started.getStatus()).isEqualTo(JobStatus.SUCCEEDED);   // 동기 실행기라 start 안에서 끝남
        assertThat(started.getRequirementId()).isEqualTo(1L);
        assertThat(started.getStartedAt()).isNotNull();
        assertThat(started.getFinishedAt()).isNotNull();

        RecommendationJob got = jobService.get(started.getId());
        assertThat(got.getResult()).isNotNull();
        assertThat(got.getResult().warnings()).isEmpty();
        assertThat(jobService.latest(1L)).get().extracting(RecommendationJob::getId).isEqualTo(started.getId());
    }

    @Test
    void 추천이_예외를_던지면_FAILED와_오류_메시지가_기록된다() {
        when(recommendationService.recommend(anyLong())).thenThrow(new IllegalStateException("엔진 오류"));

        RecommendationJob job = jobService.start(1L);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("엔진 오류");
        assertThat(job.getFinishedAt()).isNotNull();
        assertThat(jobService.get(job.getId()).getResult()).isNull();
    }

    @Test
    void 이미_진행_중인_잡이_있으면_새로_만들지_않고_그_잡을_돌려준다() {
        RecommendationJob running = new RecommendationJob();
        running.setRequirementId(1L);
        running.setStatus(JobStatus.RUNNING);
        jobMapper.insert(running);

        RecommendationJob again = jobService.start(1L);

        assertThat(again.getId()).isEqualTo(running.getId());
        assertThat(again.getStatus()).isEqualTo(JobStatus.RUNNING);   // 실행하지 않음
    }

    @Test
    void 없는_요구사항이나_잡은_404() {
        assertThatThrownBy(() -> jobService.start(9999L)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> jobService.get(9999L)).isInstanceOf(ApiException.class);
        assertThat(jobService.latest(2L)).isEmpty();
    }

    @Test
    void 서버_재시작_시_끝나지_못한_잡은_FAILED로_정리된다() {
        RecommendationJob running = new RecommendationJob();
        running.setRequirementId(2L);
        running.setStatus(JobStatus.RUNNING);
        jobMapper.insert(running);

        jobService.failInterruptedJobs();

        RecommendationJob got = jobService.get(running.getId());
        assertThat(got.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(got.getErrorMessage()).contains("재시작");
    }

    /** 후보 없는 빈 결과 */
    private static class RuleBasedStub {
        RecommendationResult result() {
            return new RecommendationResult(java.util.List.of(), java.util.List.of());
        }
    }
}
