package com.aitms.domain.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** 가입된 사용자 목록 — 담당자/실행자 선택(자유입력 콤보박스)용. 권한 체계 도입 전이라 로그인만 하면 누구나 조회 가능 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @GetMapping("/api/users")
    public List<UserSummary> list() {
        return userMapper.findAllRegistered();
    }
}
