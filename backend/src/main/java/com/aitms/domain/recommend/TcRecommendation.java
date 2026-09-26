package com.aitms.domain.recommend;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.aitms.domain.testcase.TcSource;
import com.aitms.domain.testcase.TestTechnique;

/**
 * 추천 엔진이 돌려주는 TC 후보 — 채택 시 test_case 에 review_status=DRAFT 로 저장.
 * 규칙기반(RULE)은 '파라미터화 TC 1개 + 데이터셋 N행' 형태로 생성.
 *
 * @param source          RULE(규칙 카탈로그) / RAG(과거 프로젝트 유사 TC) / LLM(신규 생성)
 * @param steps           단계 — 텍스트의 {변수}는 datasetRows 의 paramValues 로 치환됨
 * @param originProjectId RAG 인 경우 원본 프로젝트
 * @param score           0~1 신뢰도/유사도 (규칙기반은 1)
 */
public record TcRecommendation(
        Long atomicRequirementId,
        String title,
        TestTechnique technique,
        TcSource source,
        boolean parameterized,
        List<Step> steps,
        List<DatasetRow> datasetRows,
        Long originProjectId,
        BigDecimal score) {

    public record Step(String action, String expectedResult) {
    }

    public record DatasetRow(String rowLabel, Map<String, Object> paramValues, String expectedResultOverride) {
    }
}
