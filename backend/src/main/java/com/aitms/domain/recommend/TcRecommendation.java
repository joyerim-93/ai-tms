package com.aitms.domain.recommend;

import java.math.BigDecimal;
import java.util.List;

import com.aitms.domain.testcase.TcSource;
import com.aitms.domain.testcase.TestTechnique;

/**
 * 추천 엔진이 돌려주는 TC 후보. 채택 시 test_case 에 review_status=DRAFT 로 저장 예정.
 *
 * @param source          RULE(규칙 카탈로그) / RAG(과거 프로젝트 유사 TC) / LLM(신규 생성)
 * @param originProjectId RAG 인 경우 원본 프로젝트
 * @param score           0~1 신뢰도/유사도
 */
public record TcRecommendation(
        Long atomicRequirementId,
        String title,
        List<String> steps,
        String expectedResult,
        TestTechnique technique,
        TcSource source,
        Long originProjectId,
        BigDecimal score) {
}
