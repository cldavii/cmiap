package ai.incorp.cmiap.agent.content;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseChatMemoryStoreTest {

    private final ContentConversationRepository repository = mock(ContentConversationRepository.class);
    private final DatabaseChatMemoryStore store = new DatabaseChatMemoryStore(repository);

    @Test
    void persistsMessagesForTheConversationAndOwnerInTheMemoryStore() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        ContentConversation conversation = new ContentConversation(userId, "Test conversation");
        when(repository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        List<ChatMessage> messages = List.of(UserMessage.from("A separate conversation"));

        store.updateMessages(userId + ":" + conversationId, messages);

        verify(repository).save(conversation);
        assertEquals(messages, store.getMessages(userId + ":" + conversationId));
    }

    @Test
    void refusesToReadMemoryForAnotherOwner() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(repository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> store.getMessages(userId + ":" + conversationId));
    }
}
