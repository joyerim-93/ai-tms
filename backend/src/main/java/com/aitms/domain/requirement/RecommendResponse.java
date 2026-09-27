package com.aitms.domain.requirement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.aitms.domain.testcase.TestCase;

/**
 * 추천 실행 결과.
 *
 * @param created  새로 만든 DRAFT TC
 * @param skipped  같은 추천 TC가 이미 있어 건너뛴 수
 * @param warnings 후보를 만들지 못한 원자 요구사항 사유
 * @param scores   RAG 로 만든 TC의 유사도(0~1) — key = 생성된 TC id
 */
public record RecommendResponse(List<TestCase> created, int skipped, List<String> warnings, Map<Long, BigDecimal> scores) {
}
