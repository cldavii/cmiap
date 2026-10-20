package ai.incorptech.cmiap.infrastructure.persistence;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class PostgresChatMemoryStore implements ChatMemoryStore {

    private final ChatMemoryRecordRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = key(memoryId);
        return repository.findById(key)
                .map(record -> ChatMessageDeserializer.messagesFromJson(record.getMessages()))
                .orElseGet(List::of);
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        repository.upsert(key(memoryId), ChatMessageSerializer.messagesToJson(messages));
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        repository.deleteById(key(memoryId));
    }

    private static String key(Object memoryId) {
        return Objects.requireNonNull(memoryId, "O ID da memória é obrigatório").toString();
    }
}
