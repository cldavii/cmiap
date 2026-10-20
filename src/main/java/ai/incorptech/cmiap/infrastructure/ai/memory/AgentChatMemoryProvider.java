package ai.incorptech.cmiap.infrastructure.ai.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

public class AgentChatMemoryProvider implements ChatMemoryProvider {

    private final String agentId;
    private final ChatMemoryStore chatMemoryStore;
    private final int maxMessages;

    public AgentChatMemoryProvider(String agentId, ChatMemoryStore chatMemoryStore, int maxMessages) {
        this.agentId = Objects.requireNonNull(agentId);
        this.chatMemoryStore = Objects.requireNonNull(chatMemoryStore);
        if (agentId.isBlank()) {
            throw new IllegalArgumentException("O identificador do agente não pode estar vazio.");
        }
        if (maxMessages < 1) {
            throw new IllegalArgumentException("A memória deve manter ao menos uma mensagem.");
        }
        this.maxMessages = maxMessages;
    }

    @Override
    public ChatMemory get(Object memoryId) {
        String scopedMemoryId = agentId + ":" + Objects.requireNonNull(memoryId);
        return MessageWindowChatMemory.builder()
                .id(scopedMemoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    public String memoryIdFor(String userIdentity, UUID conversationId) {
        Objects.requireNonNull(userIdentity);
        Objects.requireNonNull(conversationId);
        if (userIdentity.isBlank()) {
            throw new IllegalArgumentException("A identidade do usuário não pode estar vazia.");
        }
        return "user:" + sha256(userIdentity) + ":conversation:" + conversationId;
    }

    public void clearConversation(String userIdentity, UUID conversationId) {
        get(memoryIdFor(userIdentity, conversationId)).clear();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível nesta JVM.", exception);
        }
    }
}
