package com.aitms.recommend;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.requirement.RequirementType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleCatalogService {

    private final RuleCatalogMapper mapper;

    public List<RuleCatalog> findAll() {
        return mapper.findAll();
    }

    /** 규칙기반 추천 구현 시 사용 */
    public List<RuleCatalog> findByType(RequirementType type) {
        return mapper.findByType(type);
    }
}
