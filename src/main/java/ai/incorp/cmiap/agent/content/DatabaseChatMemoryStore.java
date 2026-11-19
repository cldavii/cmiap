package ai.incorp.cmiap.agent.content;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DatabaseChatMemoryStore implements ChatMemoryStore {

    private final ContentConversationRepository conversationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Object memoryId) {
        MemoryKey key = parseMemoryKey(memoryId);
        ContentConversation conversation = findConversation(key);
        return ChatMessageDeserializer.messagesFromJson(conversation.getMemoryJson());
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        MemoryKey key = parseMemoryKey(memoryId);
        ContentConversation conversation = findConversation(key);
        conversation.setMemoryJson(ChatMessageSerializer.messagesToJson(messages));
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        updateMessages(memoryId, List.of());
    }

    private ContentConversation findConversation(MemoryKey key) {
        return conversationRepository.findByIdAndUserId(key.conversationId(), key.userId())
                .orElseThrow(() -> new IllegalStateException("A conversa da memória não existe ou não pertence ao usuário."));
    }

    private MemoryKey parseMemoryKey(Object memoryId) {
        if (!(memoryId instanceof String value)) {
            throw new IllegalArgumentException("O identificador da memória deve ser uma string.");
        }
        String[] parts = value.split(":", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("O identificador da memória da conversa é inválido.");
        }
        try {
            return new MemoryKey(UUID.fromString(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("O identificador da memória da conversa é inválido.", exception);
        }
    }

    private record MemoryKey(UUID userId, UUID conversationId) {
    }
}
