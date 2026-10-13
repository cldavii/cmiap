package ai.incorptech.cmiap.infrastructure;

import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publica o cliente de IA da plataforma. Hoje o provedor e o Google Gemini via langchain4j;
 * o resto da aplicacao depende apenas de {@link ChatModel}, entao trocar de provedor
 * nao alcanca controllers nem services.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class AiIntegration {

	@Bean
	ChatModel geminiChatModel(
			GeminiProperties properties,
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> requestFactoryBuilder) {

		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			throw new IllegalStateException(
					"Chave da API do Gemini ausente: defina a variavel de ambiente GEMINI_API_KEY "
							+ "ou a propriedade 'gemini.api-key'.");
		}

		// O classpath tem dois HttpClientBuilder (spring-restclient e jdk). Sem escolher um
		// explicitamente o langchain4j aborta a subida com "multiple HTTP clients in the classpath".
		// A fabrica HTTP vem do Spring Boot, entao 'spring.http.clients.imperative.factory' continua valendo.
		SpringRestClientBuilder httpClientBuilder = new SpringRestClientBuilder()
				.clientHttpRequestFactoryBuilder(
						requestFactoryBuilder.getIfAvailable(ClientHttpRequestFactoryBuilder::detect));

		return GoogleAiGeminiChatModel.builder()
				.httpClientBuilder(httpClientBuilder)
				.apiKey(properties.apiKey())
				.modelName(properties.modelName())
				.temperature(properties.temperature())
				.maxOutputTokens(properties.maxOutputTokens())
				.timeout(properties.timeout())
				.maxRetries(properties.maxRetries())
				.logRequestsAndResponses(properties.logRequestsAndResponses())
				.build();
	}
}
