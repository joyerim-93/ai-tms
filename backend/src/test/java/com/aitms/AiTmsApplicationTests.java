package com.aitms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:contexttest;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
class AiTmsApplicationTests {

	@Test
	void contextLoads() {
	}

}
