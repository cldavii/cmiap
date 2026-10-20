package ai.incorptech.cmiap.infrastructure.ai.memory;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfiguration {

    @Bean
    AgentChatMemoryProvider contentGeneratorChatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        return new AgentChatMemoryProvider("content-generator", chatMemoryStore, 20);
    }
}
