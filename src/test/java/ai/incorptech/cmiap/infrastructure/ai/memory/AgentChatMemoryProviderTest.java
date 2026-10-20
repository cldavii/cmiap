package ai.incorptech.cmiap.infrastructure.ai.memory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgentChatMemoryProviderTest {

    @Test
    void namespacesTheSameConversationSeparatelyForEachAgent() {
        var store = new InMemoryChatMemoryStore();
        var contentProvider = new AgentChatMemoryProvider("content-generator", store, 20);
        var noticesProvider = new AgentChatMemoryProvider("public-notices", store, 20);
        UUID conversationId = UUID.randomUUID();
        String conversationKey = contentProvider.memoryIdFor("writer@example.com", conversationId);

        Object contentMemoryId = contentProvider.get(conversationKey).id();
        Object noticesMemoryId = noticesProvider.get(conversationKey).id();
        contentProvider.get(conversationKey).add(UserMessage.from("Contexto privado do agente de conteúdo"));
        var noticesMemory = noticesProvider.get(conversationKey);

        assertThat(contentMemoryId).isEqualTo("content-generator:" + conversationKey);
        assertThat(noticesMemoryId).isEqualTo("public-notices:" + conversationKey);
        assertThat(contentMemoryId).isNotEqualTo(noticesMemoryId);
        assertThat(noticesMemory.messages()).isEmpty();
    }

    @Test
    void scopesConversationIdsToTheAuthenticatedUser() {
        var provider = new AgentChatMemoryProvider("content-generator", new InMemoryChatMemoryStore(), 20);
        UUID conversationId = UUID.randomUUID();

        String firstUserMemoryId = provider.memoryIdFor("first@example.com", conversationId);
        String secondUserMemoryId = provider.memoryIdFor("second@example.com", conversationId);

        assertThat(firstUserMemoryId).isNotEqualTo(secondUserMemoryId);
        assertThat(firstUserMemoryId).doesNotContain("first@example.com");
    }

    @Test
    void createsSlidingWindowMemoryConnectedToTheStore() {
        var provider = new AgentChatMemoryProvider("content-generator", new InMemoryChatMemoryStore(), 20);

        assertThat(provider.get("memory-1"))
                .isInstanceOf(MessageWindowChatMemory.class);
    }
}
