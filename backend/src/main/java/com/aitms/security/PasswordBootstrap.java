package com.aitms.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.aitms.domain.user.User;
import com.aitms.domain.user.UserMapper;

import lombok.RequiredArgsConstructor;

/** 비밀번호가 없는 사용자(샘플 데이터)에게 기동 시 초기 비밀번호(app.security.initial-password)를 BCrypt 로 설정. */
@Component
@RequiredArgsConstructor
public class PasswordBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordBootstrap.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.initial-password:}")
    private String initialPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (initialPassword == null || initialPassword.isBlank()) {
            return;
        }
        List<User> users = userMapper.findWithoutPassword();
        for (User u : users) {
            userMapper.updatePassword(u.getId(), passwordEncoder.encode(initialPassword));
        }
        if (!users.isEmpty()) {
            log.warn("비밀번호가 없던 사용자 {}명에게 초기 비밀번호를 설정했습니다 — 개발용입니다. 운영에서는 변경하세요.", users.size());
        }
    }
}
