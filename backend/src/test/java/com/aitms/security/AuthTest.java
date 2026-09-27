package com.aitms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:authtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@Transactional
class AuthTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private static final String PASSWORD = "aitms1234!";   // application.yml app.security.initial-password
    private static final String TC_BODY = "{\"projectId\":1,\"title\":\"작성자 확인\",\"priority\":\"MEDIUM\",\"steps\":[]}";

    private MockHttpSession login(String loginId, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    @Test
    void 로그인_없이는_API가_401이고_메시지를_돌려준다() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void 잘못된_비밀번호_없는_계정은_같은_401이고_빈_값은_400() throws Exception {
        for (String[] bad : new String[][] {{"qa.kim", "wrong"}, {"nobody", PASSWORD}}) {
            mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"loginId\":\"" + bad[0] + "\",\"password\":\"" + bad[1] + "\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
        }
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 샘플_사용자는_초기_비밀번호로_로그인하고_세션으로_me와_API를_쓸_수_있다() throws Exception {
        MockHttpSession session = login("qa.kim", PASSWORD);

        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("qa.kim")).andExpect(jsonPath("$.displayName").value("김큐에이"))
                .andExpect(jsonPath("$.role").value("QA")).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/projects").session(session)).andExpect(status().isOk());
    }

    @Test
    void 작성자는_로그인한_사용자로_기록된다() throws Exception {
        MockHttpSession dev = login("dev.park", PASSWORD);
        mvc.perform(post("/api/test-cases").session(dev).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(TC_BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.authorName").value("박개발"));

        MockHttpSession qa = login("qa.kim", PASSWORD);
        mvc.perform(post("/api/test-cases").session(qa).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(TC_BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.authorName").value("김큐에이"));
    }

    @Test
    void CSRF_토큰_없는_변경_요청은_403() throws Exception {
        MockHttpSession session = login("qa.kim", PASSWORD);
        mvc.perform(post("/api/test-cases").session(session).contentType(MediaType.APPLICATION_JSON).content(TC_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void 로그아웃하면_세션이_끝나_다시_401() throws Exception {
        MockHttpSession session = login("qa.kim", PASSWORD);
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void 비밀번호_없는_임시_계정은_로그인할_수_없다() throws Exception {
        jdbc.update("INSERT INTO users (login_id, name, role) VALUES ('guest-abcd1234', '임시', 'QA')");
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"guest-abcd1234\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void H2_콘솔은_ADMIN만() throws Exception {
        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
        assertThat(login("qa.kim", PASSWORD)).isNotNull();
        mvc.perform(get("/h2-console/").session(login("qa.kim", PASSWORD))).andExpect(status().isForbidden());
    }

    @Test
    void 시드_계정_3171613으로_로그인할_수_있고_username_필드도_받는다() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"3171613\",\"password\":\"3171613\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("3171613"));
        assertThat(jdbc.queryForObject("SELECT password FROM users WHERE login_id = '3171613'", String.class)).startsWith("$2a$");
    }

    private org.springframework.test.web.servlet.ResultActions register(String username, String password, String displayName) throws Exception {
        return mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"display_name\":\"" + displayName + "\"}"));
    }

    @Test
    void 회원가입하면_BCrypt로_저장되고_바로_로그인할_수_있다() throws Exception {
        register("newbie01", "pass1234!", "새내기").andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newbie01")).andExpect(jsonPath("$.displayName").value("새내기"))
                .andExpect(jsonPath("$.role").value("QA")).andExpect(jsonPath("$.password").doesNotExist());
        String hash = jdbc.queryForObject("SELECT password FROM users WHERE login_id = 'newbie01'", String.class);
        assertThat(hash).startsWith("$2a$").doesNotContain("pass1234!");

        MockHttpSession session = login("newbie01", "pass1234!");
        mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.displayName").value("새내기"));
    }

    @Test
    void 회원가입_검증_중복_아이디_짧은_비밀번호_잘못된_아이디() throws Exception {
        register("qa.kim", "pass1234!", "중복").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."));
        register("ok_user", "short", "짧음").andExpect(status().isBadRequest());
        register("bad user!", "pass1234!", "공백").andExpect(status().isBadRequest());
        register("ab", "pass1234!", "짧은 아이디").andExpect(status().isBadRequest());
        register("nameless", "pass1234!", " ").andExpect(status().isBadRequest());
    }

    @Test
    void 회원가입은_로그인_없이_가능하지만_CSRF는_필요하다() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nocsrf\",\"password\":\"pass1234!\",\"displayName\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void CORS는_프론트_출처에_자격증명_쿠키를_허용한다() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/auth/login")
                        .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(get("/api/auth/me").header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));   // 401 응답에도 CORS 헤더가 있어야 브라우저가 읽을 수 있음
    }
}
