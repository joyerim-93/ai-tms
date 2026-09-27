package com.aitms.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.aitms.domain.user.User;

/** 로그인한 사용자(세션에 저장되는 principal). 역할: DEV / BIZ / QA / ADMIN → ROLE_xxx */
public class AuthUser implements UserDetails {

    private final Long id;
    private final String loginId;
    private final String name;
    private final String role;
    private final String passwordHash;
    private final boolean enabled;

    public AuthUser(User u) {
        this.id = u.getId();
        this.loginId = u.getLoginId();
        this.name = u.getName();
        this.role = u.getRole();
        this.passwordHash = u.getPassword();
        this.enabled = u.isEnabled();
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
