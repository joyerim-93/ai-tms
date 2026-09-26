package com.aitms;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:schematest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
class SchemaInitTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 모든_테이블이_생성된다() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'", Integer.class);
        assertThat(count).isEqualTo(13);
    }

    @Test
    void 샘플_데이터는_재실행해도_중복_덮어쓰기_없고_신규_INSERT가_id충돌없이_된다() {
        // 사용자가 샘플 수행 결과를 바꾼 뒤 재기동해도 되돌아가지 않아야 함
        jdbc.update("UPDATE test_execution SET result = 'PASS' WHERE id = 7");

        // 재기동 상황 재현: schema.sql + data.sql 한 번 더 실행
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql"), new ClassPathResource("data.sql"))
                .execute(dataSource);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id <= 5", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM project_member", Integer.class)).isEqualTo(5);

        assertThat(jdbc.queryForObject("SELECT result FROM test_execution WHERE id = 7", String.class)).isEqualTo("PASS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM test_case", Integer.class)).isEqualTo(11);

        jdbc.update("INSERT INTO users (login_id, name, role) VALUES ('new01', '신규', 'DEV')");
        Long newId = jdbc.queryForObject("SELECT id FROM users WHERE login_id = 'new01'", Long.class);
        assertThat(newId).isGreaterThan(5);
    }
}
