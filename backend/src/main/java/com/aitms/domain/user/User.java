package com.aitms.domain.user;

import lombok.Getter;
import lombok.Setter;

/** users 행 (로그인 계정) */
@Getter
@Setter
public class User {
    private Long id;
    private String loginId;
    private String name;
    private String role;      // DEV | BIZ | QA | ADMIN
    private String password;  // BCrypt 해시, 없으면 로그인 불가
    private boolean enabled;
}
