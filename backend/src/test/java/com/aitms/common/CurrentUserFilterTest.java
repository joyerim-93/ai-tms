package com.aitms.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.aitms.domain.user.UserService;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:usertest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@Transactional
class CurrentUserFilterTest {

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired JdbcTemplate jdbc;

    private static final String BODY = "{\"projectId\":1,\"title\":\"작성자 확인\",\"priority\":\"MEDIUM\",\"steps\":[]}";

    @Test
    void 이름으로_사용자를_찾거나_없으면_임시_사용자를_만든다() {
        assertThat(userService.resolveByName("김큐에이")).isEqualTo(1L);     // 샘플 사용자와 이름이 같으면 그 사용자

        Long a = userService.resolveByName("  홍길동 ");
        assertThat(userService.resolveByName("홍길동")).isEqualTo(a);         // 같은 이름은 재사용
        assertThat(jdbc.queryForObject("SELECT login_id FROM users WHERE id = ?", String.class, a)).startsWith("guest-");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE name = '홍길동'", Integer.class)).isEqualTo(1);
    }

    @Test
    void X_User_Name_헤더의_이름이_그_요청의_작성자가_된다() throws Exception {
        mvc.perform(post("/api/test-cases").contentType(MediaType.APPLICATION_JSON).content(BODY)
                        .header("X-User-Name", URLEncoder.encode("박담당", StandardCharsets.UTF_8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("박담당"));
    }

    @Test
    void 헤더가_없으면_기본_사용자이고_요청이_끝나면_초기화된다() throws Exception {
        mvc.perform(post("/api/test-cases").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("김큐에이"));
        assertThat(CurrentUser.id()).isEqualTo(1L);
    }
}
