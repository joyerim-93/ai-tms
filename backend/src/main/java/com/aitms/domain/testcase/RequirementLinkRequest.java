package com.aitms.domain.testcase;

import java.util.List;

/** 연결 요구사항 전체 교체 (빈 목록 = 모두 해제) */
public record RequirementLinkRequest(List<Long> atomicRequirementIds) {
}
