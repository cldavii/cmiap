package ai.incorptech.cmiap.config;

import ai.incorptech.cmiap.modules.tutor.service.AssistantAiService;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiAgentsConfig {

    @Value("${gemini.api-key}")
    private String GEMINI_API;

    @Value("${gemini.model}")
    private String GEMINI_MODEL;

    @Bean
    public GoogleAiGeminiChatModel googleAiGeminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(GEMINI_API)
                .modelName(GEMINI_MODEL)
                .build();
    }

    @Bean
    public AssistantAiService assistant(GoogleAiGeminiChatModel model) {
        return AiServices.builder(AssistantAiService.class)
                .chatModel(model)
                .build();
    }

    @Bean
    public ai.incorptech.cmiap.modules.editais.service.EditalAiService editalAssistant(GoogleAiGeminiChatModel model) {
        return AiServices.builder(ai.incorptech.cmiap.modules.editais.service.EditalAiService.class)
                .chatModel(model)
                .build();
    }
}
