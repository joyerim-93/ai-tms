package com.aitms.domain.recommend;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 한글 친화 텍스트 유사도 — 토큰 안의 글자 bigram 집합의 Dice 계수 (0~1).
 * 조사·어미가 붙어도('우대금리가' / '우대금리') 겹치는 bigram 으로 매칭됨. 숫자('0.2')는 통째로 1토큰.
 * 임베딩(ai-agent) 도입 시 {@link TcRetriever} 구현체째 교체하므로 이 클래스는 키워드 검색기 전용.
 */
final class TextSimilarity {

    private static final Pattern TOKEN = Pattern.compile("\\p{N}+(?:\\.\\p{N}+)?|\\p{L}+");

    private TextSimilarity() {
    }

    static Set<String> features(String text) {
        Set<String> out = new HashSet<>();
        if (text == null) {
            return out;
        }
        Matcher m = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            String t = m.group();
            if (t.length() < 2 || Character.isDigit(t.charAt(0))) {
                out.add(t);
                continue;
            }
            for (int i = 0; i < t.length() - 1; i++) {
                out.add(t.substring(i, i + 2));
            }
        }
        return out;
    }

    static double dice(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int common = 0;
        for (String f : a) {
            if (b.contains(f)) {
                common++;
            }
        }
        return 2.0 * common / (a.size() + b.size());
    }
}
