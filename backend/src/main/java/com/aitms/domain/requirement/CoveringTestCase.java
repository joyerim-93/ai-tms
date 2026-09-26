package com.aitms.domain.requirement;

import lombok.Getter;
import lombok.Setter;

/** 원자 요구사항을 커버하는 TC (Traceability 뷰). 상태 값은 문자열로 둬 testcase 패키지 의존을 피함 */
@Getter
@Setter
public class CoveringTestCase {
    private Long atomicRequirementId; // 그룹핑용
    private Long id;
    private String tcCode;
    private String title;
    private String source;
    private String reviewStatus;
    private String status;
}
