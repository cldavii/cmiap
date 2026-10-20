package ai.incorptech.cmiap.infrastructure.persistence;

import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresChatMemoryStoreTest {

    private final ChatMemoryRecordRepository repository = mock(ChatMemoryRecordRepository.class);
    private final PostgresChatMemoryStore store = new PostgresChatMemoryStore(repository);

    @Test
    void returnsAnEmptyMemoryWhenNoRecordExists() {
        when(repository.findById("agent:conversation")).thenReturn(Optional.empty());

        assertThat(store.getMessages("agent:conversation")).isEmpty();
    }

    @Test
    void deserializesStoredLangChainMessages() {
        String serialized = ChatMessageSerializer.messagesToJson(List.of(UserMessage.from("Olá")));
        when(repository.findById("agent:conversation"))
                .thenReturn(Optional.of(new ChatMemoryRecord("agent:conversation", serialized)));

        assertThat(store.getMessages("agent:conversation"))
                .containsExactlyElementsOf(ChatMessageDeserializer.messagesFromJson(serialized));
    }

    @Test
    void serializesAndUpsertsMessages() {
        store.updateMessages("agent:conversation", List.of(UserMessage.from("Olá")));

        verify(repository).upsert(
                org.mockito.ArgumentMatchers.eq("agent:conversation"),
                org.mockito.ArgumentMatchers.contains("Olá"));
    }

    @Test
    void deletesTheRequestedMemory() {
        store.deleteMessages("agent:conversation");

        verify(repository).deleteById("agent:conversation");
    }
}
