package ai.incorp.cmiap;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.SecureRandom;
import java.util.Base64;

@SpringBootTest
class CmiapApplicationTests {

	private static final String TEST_JWT_SECRET = createTestJwtSecret();

	@DynamicPropertySource
	static void registerSecurityProperties(DynamicPropertyRegistry registry) {
		registry.add("app.security.jwt.secret", () -> TEST_JWT_SECRET);
		registry.add("langchain4j.google-ai-gemini.chat-model.api-key", () -> "test-gemini-api-key");
		registry.add("langchain4j.google-ai-gemini.chat-model.model-name", () -> "gemini-2.5-flash");
	}

	@Test
	void contextLoads() {
	}

	private static String createTestJwtSecret() {
		byte[] secret = new byte[32];
		new SecureRandom().nextBytes(secret);
		return Base64.getEncoder().encodeToString(secret);
	}

}
