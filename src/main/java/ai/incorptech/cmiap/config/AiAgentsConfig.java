package ai.incorptech.cmiap.config;

import ai.incorptech.cmiap.modules.editais.service.EditalAiService;
import ai.incorptech.cmiap.modules.tutor.service.AssistantAiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta os agentes (interfaces com @AiService) sobre o ChatModel unico da plataforma.
 *
 * O modelo vem de {@link ai.incorptech.cmiap.infrastructure.AiIntegration}, configurado por
 * {@link ai.incorptech.cmiap.infrastructure.GeminiProperties}: chave em GEMINI_API_KEY e
 * modelo em gemini.model-name. Aqui nao se le propriedade nenhuma, para nao existirem dois
 * beans do Gemini nem dois nomes para a mesma configuracao.
 */
@Configuration
public class AiAgentsConfig {

    @Bean
    public AssistantAiService assistant(ChatModel model) {
        return AiServices.builder(AssistantAiService.class)
                .chatModel(model)
                .build();
    }

    @Bean
    public EditalAiService editalAssistant(ChatModel model) {
        return AiServices.builder(EditalAiService.class)
                .chatModel(model)
                .build();
    }
}
