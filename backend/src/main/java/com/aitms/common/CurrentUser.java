package com.aitms.common;

/**
 * 현재 사용자. 지금은 인증이 없어 화면에서 입력한 이름(X-User-Name 헤더 → users 행)을 요청 스레드에 담아 씀
 * ({@link CurrentUserFilter}). 헤더가 없으면 기본 사용자(id 1).
 * TODO: Spring Security 도입 시 SecurityContext 에서 조회하도록 이 클래스만 교체 (호출부는 그대로).
 */
public final class CurrentUser {

    private static final Long DEFAULT_USER_ID = 1L; // data.sql 의 qa.kim
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static Long id() {
        Long id = HOLDER.get();
        return id != null ? id : DEFAULT_USER_ID;
    }

    static void set(Long userId) {
        HOLDER.set(userId);
    }

    static void clear() {
        HOLDER.remove();
    }
}
