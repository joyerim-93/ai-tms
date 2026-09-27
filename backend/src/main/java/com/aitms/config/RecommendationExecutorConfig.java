package com.aitms.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** AI 추천 잡을 백그라운드에서 실행 (RAG/LLM 등 오래 걸릴 엔진 대비). */
@Configuration
public class RecommendationExecutorConfig {

    @Bean("recommendationExecutor")
    public Executor recommendationExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("recommend-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(10);
        return ex;
    }
}
