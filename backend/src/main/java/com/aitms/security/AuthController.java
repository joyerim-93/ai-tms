package com.aitms.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aitms.common.ApiException;
import com.aitms.domain.user.User;
import com.aitms.domain.user.UserMapper;
import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/** 로그인 / 현재 사용자. 로그아웃은 SecurityConfig 의 POST /api/auth/logout (204). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public record LoginRequest(@JsonAlias("loginId") @NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "아이디는 영문·숫자·. _ - 만 사용할 수 있습니다.") String username,
            @NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.") String password,
            @JsonAlias("display_name") @NotBlank @Size(max = 50) String displayName) {
    }

    /** 로그인 사용자 — username(=users.login_id), displayName(=users.name) */
    public record UserResponse(Long id, String username, String displayName, String role) {
        static UserResponse from(AuthUser u) {
            return new UserResponse(u.id(), u.getUsername(), u.name(), u.role());
        }
    }

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.registration-enabled:true}")
    private boolean registrationEnabled;

    @PostMapping("/login")
    public UserResponse login(@Validated @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username().strip(), body.password()));
        } catch (AuthenticationException e) {
            // 아이디 없음/비밀번호 불일치/비활성 모두 같은 메시지 (계정 존재 여부 노출 방지)
            throw new ApiException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (request.getSession(false) != null) {
            request.changeSessionId(); // 세션 고정 공격 방지
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return UserResponse.from((AuthUser) auth.getPrincipal());
    }

    /** 회원가입 — 기본 역할 QA, 비밀번호는 BCrypt. 로그인은 하지 않으므로 화면이 이어서 /login 을 호출 */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Validated @RequestBody RegisterRequest body) {
        if (!registrationEnabled) {
            throw new ApiException(HttpStatus.FORBIDDEN, "회원가입이 비활성화되어 있습니다.");
        }
        String username = body.username().strip();
        if (userMapper.findByLoginId(username).isPresent()) {
            throw ApiException.conflict("이미 사용 중인 아이디입니다.");
        }
        User user = new User();
        user.setLoginId(username);
        user.setName(body.displayName().strip());
        user.setRole("QA");
        user.setPassword(passwordEncoder.encode(body.password()));
        userMapper.insert(user);
        return new UserResponse(user.getId(), username, user.getName(), user.getRole());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return UserResponse.from(user);
    }
}
