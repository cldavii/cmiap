package ai.incorptech.cmiap.configuration;

import ai.incorptech.cmiap.service.AssistantAiService;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfiguration {

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
}
