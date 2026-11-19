package ai.incorp.cmiap;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.cookies.secure=true",
        "app.security.cookies.same-site="
})
class CmiapApplicationTests {
	@Value("${local.server.port}")
	private int port;


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

	@Test
	void csrfSessionCookieSupportsTheHostedFrontend() throws Exception {
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/csrf"))
				.header("Origin", "https://incorp-front.onrender.com")
				.build();
		try (var client = HttpClient.newHttpClient()) {
			var response = client.send(request, HttpResponse.BodyHandlers.discarding());
			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.headers().allValues("Set-Cookie")).anySatisfy(cookie -> {
				assertThat(cookie).startsWith("JSESSIONID=").contains("; Secure", "; HttpOnly", "; SameSite=None");
			});
		}
	}

	private static String createTestJwtSecret() {
		byte[] secret = new byte[32];
		new SecureRandom().nextBytes(secret);
		return Base64.getEncoder().encodeToString(secret);
	}

}
