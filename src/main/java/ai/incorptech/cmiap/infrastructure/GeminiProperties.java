package ai.incorptech.cmiap.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parametros da integracao com o Google Gemini, prefixo {@code gemini} no application.yaml.
 *
 * @param apiKey chave da API do Google AI Studio; vem da variavel de ambiente GEMINI_API_KEY
 * @param modelName modelo usado nas chamadas de chat
 * @param temperature grau de criatividade da geracao (0.0 a 2.0)
 * @param maxOutputTokens teto de tokens na resposta
 * @param timeout tempo maximo de espera por uma resposta
 * @param logRequestsAndResponses registra no log o corpo das chamadas; manter desligado fora de dev
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
		String apiKey,
		@DefaultValue("gemini-3.8-flash") String modelName,
		@DefaultValue("0.7") Double temperature,
		@DefaultValue("2048") Integer maxOutputTokens,
		@DefaultValue("60s") Duration timeout,
		@DefaultValue("false") Boolean logRequestsAndResponses) {
}
