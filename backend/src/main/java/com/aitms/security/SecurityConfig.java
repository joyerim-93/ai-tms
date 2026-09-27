package com.aitms.security;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 세션 기반 로그인(JSESSIONID) — SPA 는 /api/auth/login 으로 로그인.
 * CSRF: XSRF-TOKEN 쿠키 → 프론트가 X-XSRF-TOKEN 헤더로 되돌려 보냄. /api/** 는 로그인 필수, /h2-console 은 ADMIN 만.
 * 그 외(정적 SPA 껍데기 — index.html/JS/CSS, {@link com.aitms.config.SpaForwardController}의 포워드 대상)는 공개:
 * 실제 데이터 보호는 /api/** 인증이 하고, 화면 단위 보호는 프론트 라우터 가드가 함(단일 jar 배포 시 필요 — 로그인 페이지 자체가
 * 인증을 요구하면 아무도 로그인할 수 없음).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository contextRepository) throws Exception {
        http
                .cors(Customizer.withDefaults())   // WebConfig 의 CORS(자격증명 포함) 설정을 인증 필터 앞에서도 적용
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/h2-console/**"))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .securityContext(c -> c.securityContextRepository(contextRepository))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/error").permitAll()
                        .requestMatchers("/h2-console/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(l -> l
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value())))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))   // H2 콘솔 프레임
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthorized())
                        .accessDeniedHandler(forbidden()));
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthUserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    private static AuthenticationEntryPoint unauthorized() {
        return (req, res, ex) -> json(res, HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }

    private static AccessDeniedHandler forbidden() {
        return (req, res, ex) -> json(res, HttpStatus.FORBIDDEN, "권한이 없거나 요청을 확인할 수 없습니다.");
    }

    private static void json(HttpServletResponse res, HttpStatus status, String message) throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
