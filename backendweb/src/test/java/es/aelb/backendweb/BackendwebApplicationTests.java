package es.aelb.backendweb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "aelb.jwt.secret=test-only-secret-with-at-least-32-bytes")
class BackendwebApplicationTests {

	@Test
	void contextLoads() {
	}

}
