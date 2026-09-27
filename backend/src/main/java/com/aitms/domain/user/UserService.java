package com.aitms.domain.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    static final int MAX_NAME = 50;

    private final UserMapper mapper;

    /**
     * 화면에서 입력한 이름 → users.id. 같은 이름이 있으면 그 사용자, 없으면 임시 사용자(login_id=guest-xxxx, 역할 QA)를 만듦.
     * 인증이 아닌 표시용이라 Spring Security 도입 때 실제 계정으로 교체 대상.
     */
    @Transactional
    public Long resolveByName(String name) {
        String n = name.strip();
        if (n.length() > MAX_NAME) {
            n = n.substring(0, MAX_NAME);
        }
        String finalName = n;
        return mapper.findIdByName(finalName).orElseGet(() -> {
            String loginId = "guest-" + UUID.randomUUID().toString().substring(0, 8);
            mapper.insertGuest(loginId, finalName);
            return mapper.findIdByLoginId(loginId).orElseThrow();
        });
    }
}
