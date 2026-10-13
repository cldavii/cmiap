package ai.incorptech.cmiap.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiIntegrationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ImperativeHttpClientAutoConfiguration.class))
			.withPropertyValues("spring.http.clients.imperative.factory=simple")
			.withUserConfiguration(AiIntegration.class);

	@Test
	void publicaUmChatModelDoGeminiComOModeloConfigurado() {
		contextRunner
				.withPropertyValues(
						"gemini.api-key=chave-de-teste",
						"gemini.model-name=gemini-3.8-flash")
				.run(context -> {
					assertThat(context).hasSingleBean(ChatModel.class);

					ChatModel chatModel = context.getBean(ChatModel.class);
					assertThat(chatModel).isInstanceOf(GoogleAiGeminiChatModel.class);
					assertThat(chatModel.defaultRequestParameters().modelName())
							.isEqualTo("gemini-3.8-flash");
				});
	}

	@Test
	void usaGemini38FlashComoModeloPadrao() {
		contextRunner
				.withPropertyValues("gemini.api-key=chave-de-teste")
				.run(context -> assertThat(context.getBean(ChatModel.class).defaultRequestParameters().modelName())
						.isEqualTo("gemini-3.8-flash"));
	}

	@Test
	void falhaNaSubidaQuandoAChaveDaApiNaoEstaDefinida() {
		contextRunner
				.withPropertyValues("gemini.api-key=")
				.run(context -> assertThat(context)
						.hasFailed()
						.getFailure()
						.hasMessageContaining("GEMINI_API_KEY"));
	}

	@Test
	void aplicaOsParametrosDeGeracaoConfigurados() {
		contextRunner
				.withPropertyValues(
						"gemini.api-key=chave-de-teste",
						"gemini.temperature=0.2",
						"gemini.max-output-tokens=512")
				.run(context -> {
					var parametros = context.getBean(ChatModel.class).defaultRequestParameters();
					assertThat(parametros.temperature()).isEqualTo(0.2);
					assertThat(parametros.maxOutputTokens()).isEqualTo(512);
				});
	}
}
