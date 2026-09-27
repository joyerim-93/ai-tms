package com.aitms.domain.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** GET /api/users — 담당자 선택용 가입 사용자 목록 (테스트수행 TC 추가 화면) */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:userlisttest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private MockHttpSession login() throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"qa.kim\",\"password\":\"aitms1234!\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    @Test
    void 로그인하면_가입된_사용자_전체를_이름순으로_볼_수_있고_비밀번호없는_임시계정은_빠진다() throws Exception {
        jdbc.update("INSERT INTO users (login_id, name, role) VALUES ('guest-zzzz1111', '임시사용자', 'QA')"); // 비밀번호 없음 → 제외
        jdbc.update("UPDATE users SET enabled = FALSE WHERE login_id = 'admin'"); // 비활성 → 제외

        mvc.perform(get("/api/users").session(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].displayName").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("임시사용자"))))
                .andExpect(jsonPath("$[*].displayName").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("관리자"))))
                .andExpect(jsonPath("$[*].displayName").value(org.hamcrest.Matchers.hasItems("김큐에이", "박개발")));
    }

    @Test
    void 로그인_없이는_401() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }
}
