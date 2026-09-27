package com.aitms.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.aitms.domain.user.User;
import com.aitms.domain.user.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User u = userMapper.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException(loginId));
        if (u.getPassword() == null) {
            throw new UsernameNotFoundException(loginId); // 비밀번호가 없는 계정은 로그인 불가
        }
        return new AuthUser(u);
    }
}
