package com.aitms.domain.recommend;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.aitms.domain.requirement.AtomicRequirement;

import lombok.RequiredArgsConstructor;

/**
 * 키워드(bigram Dice) 유사도 검색.
 * 점수 = max(원자 텍스트 ↔ TC 제목·모듈·태그, 원자 텍스트 ↔ TC가 검증하는 원자 요구사항 텍스트) + 같은 요구사항 유형 가산(+0.1), 최대 1.
 * 임계값 미만은 버림.
 */
@Component
@RequiredArgsConstructor
public class KeywordTcRetriever implements TcRetriever {

    static final double MIN_SCORE = 0.3;
    static final double TYPE_BONUS = 0.1;

    private final RagMapper ragMapper;

    @Override
    public List<Hit> search(AtomicRequirement atomic, Long excludeProjectId, int limit) {
        Set<String> query = TextSimilarity.features(atomic.getAtomicText());
        List<RagCandidate> pool = loadPool(excludeProjectId);
        return pool.stream()
                .map(c -> new Hit(c.getId(), c.getProjectId(), score(query, atomic, c)))
                .filter(h -> h.score() >= MIN_SCORE)
                .sorted(Comparator.comparingDouble(Hit::score).reversed().thenComparing(Hit::testCaseId))
                .limit(limit)
                .toList();
    }

    private double score(Set<String> query, AtomicRequirement atomic, RagCandidate c) {
        double best = TextSimilarity.dice(query,
                TextSimilarity.features(String.join(" ", nz(c.getTitle()), nz(c.getModule()), nz(c.getTags()))));
        boolean sameType = false;
        for (RagCandidate.Linked l : c.getLinked()) {
            best = Math.max(best, TextSimilarity.dice(query, TextSimilarity.features(l.atomicText())));
            sameType |= l.type() == atomic.getType();
        }
        return Math.min(1.0, best + (sameType ? TYPE_BONUS : 0));
    }

    private List<RagCandidate> loadPool(Long excludeProjectId) {
        List<RagCandidate> pool = ragMapper.findCandidates(excludeProjectId);
        java.util.Map<Long, RagCandidate> byId = new java.util.HashMap<>();
        pool.forEach(c -> byId.put(c.getId(), c));
        for (RagLink l : ragMapper.findLinks(excludeProjectId)) {
            RagCandidate c = byId.get(l.getTestCaseId());
            if (c != null) {
                c.getLinked().add(new RagCandidate.Linked(l.getAtomicText(), l.getType()));
            }
        }
        return pool;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
