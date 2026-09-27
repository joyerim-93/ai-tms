package com.aitms.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:attachtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@Transactional
class AttachmentTest {

    @TempDir static Path uploads;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.upload.dir", () -> uploads.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
    private MockHttpSession qa;

    @BeforeEach
    void login() throws Exception {
        qa = login("qa.kim");
    }

    private MockHttpSession login(String loginId) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"" + loginId + "\",\"password\":\"aitms1234!\"}")).andExpect(status().isOk()).andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "application/octet-stream", bytes);
    }

    private long uploadOne(String base, long id, MockMultipartFile f) throws Exception {
        MvcResult res = mvc.perform(multipart("/api/" + base + "/" + id + "/attachments").file(f).session(qa).with(csrf()))
                .andExpect(status().isCreated()).andReturn();
        String body = res.getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("^\\[\\{\"id\":(\\d+).*", "$1"));
    }

    @Test
    void 실행_항목에_이미지를_올리면_목록에_나오고_그대로_내려받을_수_있다() throws Exception {
        MvcResult res = mvc.perform(multipart("/api/executions/1/attachments").file(file("실패 화면.PNG", PNG)).session(qa).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].fileName").value("실패 화면.PNG"))
                .andExpect(jsonPath("$[0].contentType").value("image/png"))
                .andExpect(jsonPath("$[0].image").value(true))
                .andExpect(jsonPath("$[0].uploadedByName").value("김큐에이"))
                .andExpect(jsonPath("$[0].filePath").doesNotExist())   // 서버 경로 비노출
                .andReturn();
        long id = uploadIdFrom(res);

        mvc.perform(get("/api/executions/1/attachments").session(qa)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/executions/1/attachments/" + id + "/file").session(qa)).andExpect(status().isOk())
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")));
        mvc.perform(get("/api/executions/1/attachments/" + id + "/file?inline=true").session(qa)).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline")));

        // 파일은 디스크(uploads/executions/1/…)에, 경로는 UUID 이름
        try (Stream<Path> files = Files.walk(uploads.resolve("executions/1"))) {
            assertThat(files.filter(Files::isRegularFile)).singleElement().satisfies(p -> assertThat(p.getFileName().toString()).endsWith(".png"));
        }
    }

    private static long uploadIdFrom(MvcResult res) throws Exception {
        return Long.parseLong(res.getResponse().getContentAsString().replaceAll("^\\[\\{\"id\":(\\d+).*", "$1"));
    }

    @Test
    void 이미지가_아닌_파일은_inline_요청이어도_다운로드로_내려간다() throws Exception {
        long id = uploadOne("executions", 1, file("run.log", "error at line 3".getBytes()));
        mvc.perform(get("/api/executions/1/attachments/" + id + "/file?inline=true").session(qa)).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(content().string("error at line 3"));
    }

    @Test
    void 허용되지_않는_형식_빈_파일_개수_초과는_400이고_경로_조작_이름은_정리된다() throws Exception {
        for (String bad : new String[] {"evil.html", "x.svg", "run.exe", "noext"}) {
            mvc.perform(multipart("/api/executions/1/attachments").file(file(bad, PNG)).session(qa).with(csrf()))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/api/executions/1/attachments").file(file("empty.png", new byte[0])).session(qa).with(csrf()))
                .andExpect(status().isBadRequest());

        mvc.perform(multipart("/api/executions/1/attachments").file(file("../../etc/passwd.png", PNG)).session(qa).with(csrf()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$[0].fileName").value("passwd.png"));

        for (int i = 0; i < AttachmentService.MAX_PER_OWNER - 1; i++) {
            uploadOne("executions", 1, file("a" + i + ".png", PNG));
        }
        mvc.perform(multipart("/api/executions/1/attachments").file(file("over.png", PNG)).session(qa).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 종료된_차수의_실행_항목은_추가_삭제가_409_조회는_가능() throws Exception {
        long id = uploadOne("executions", 1, file("a.png", PNG));
        jdbc.update("UPDATE test_cycle SET status = 'CLOSED' WHERE id = (SELECT cycle_id FROM test_execution WHERE id = 1)");

        mvc.perform(multipart("/api/executions/1/attachments").file(file("b.png", PNG)).session(qa).with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/executions/1/attachments/" + id).session(qa).with(csrf())).andExpect(status().isConflict());
        mvc.perform(get("/api/executions/1/attachments").session(qa)).andExpect(status().isOk());
    }

    @Test
    void 삭제는_업로드한_사람이나_관리자만_하고_디스크_파일도_지운다() throws Exception {
        long id = uploadOne("executions", 1, file("a.png", PNG));

        mvc.perform(delete("/api/executions/1/attachments/" + id).session(login("dev.park")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/executions/1/attachments/" + id).session(login("admin")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/executions/1/attachments").session(qa)).andExpect(jsonPath("$.length()").value(0));
        try (Stream<Path> files = Files.walk(uploads.resolve("executions/1"))) {
            assertThat(files.filter(Files::isRegularFile)).isEmpty();
        }
        mvc.perform(delete("/api/executions/1/attachments/" + id).session(qa).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void 다른_항목의_첨부는_접근할_수_없고_없는_대상은_404() throws Exception {
        long id = uploadOne("executions", 1, file("a.png", PNG));
        mvc.perform(get("/api/executions/2/attachments/" + id + "/file").session(qa)).andExpect(status().isNotFound());
        mvc.perform(get("/api/executions/9999/attachments").session(qa)).andExpect(status().isNotFound());
        mvc.perform(get("/api/defects/9999/attachments").session(qa)).andExpect(status().isNotFound());
    }

    @Test
    void 결함에도_같은_방식으로_첨부할_수_있다() throws Exception {
        long id = uploadOne("defects", 1, file("재현.jpg", PNG));
        mvc.perform(get("/api/defects/1/attachments").session(qa)).andExpect(jsonPath("$[0].fileName").value("재현.jpg"))
                .andExpect(jsonPath("$[0].contentType").value("image/jpeg"));
        mvc.perform(get("/api/defects/1/attachments/" + id + "/file").session(qa)).andExpect(content().bytes(PNG));
        assertThat(Files.exists(uploads.resolve("defects/1"))).isTrue();
        mvc.perform(delete("/api/defects/1/attachments/" + id).session(qa).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void 로그인_없이는_401() throws Exception {
        mvc.perform(get("/api/executions/1/attachments")).andExpect(status().isUnauthorized());
    }
}
