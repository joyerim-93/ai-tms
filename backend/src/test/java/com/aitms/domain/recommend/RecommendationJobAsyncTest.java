package com.aitms.domain.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.aitms.domain.testcase.ReviewStatus;

/** 실제 백그라운드 스레드 — 트랜잭션 없이(커밋된 데이터로) start → 폴링 → SUCCEEDED */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:jobasync;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
class RecommendationJobAsyncTest {

    @Autowired RecommendationJobService jobService;

    @Test
    void 요청은_즉시_잡을_돌려주고_백그라운드에서_완료되어_DRAFT_TC가_생긴다() {
        RecommendationJob started = jobService.start(1L);

        assertThat(started.getId()).isNotNull();
        assertThat(started.getStatus()).isIn(JobStatus.RUNNING, JobStatus.SUCCEEDED);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(
                () -> assertThat(jobService.get(started.getId()).getStatus()).isEqualTo(JobStatus.SUCCEEDED));

        RecommendationJob done = jobService.get(started.getId());
        assertThat(done.getResult().created()).isNotEmpty();
        assertThat(done.getResult().created()).allSatisfy(tc -> assertThat(tc.getReviewStatus()).isEqualTo(ReviewStatus.DRAFT));
    }
}
