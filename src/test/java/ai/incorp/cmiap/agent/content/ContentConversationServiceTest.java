package ai.incorp.cmiap.agent.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentConversationServiceTest {

    private ContentConversationRepository conversationRepository;
    private ContentConversationMessageRepository messageRepository;
    private ObjectMapper objectMapper;
    private ContentConversationService service;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ContentConversationRepository.class);
        messageRepository = mock(ContentConversationMessageRepository.class);
        objectMapper = new ObjectMapper();
        service = new ContentConversationService(conversationRepository, messageRepository, objectMapper);
    }

    @Test
    void getOrCreateCreatesTruncatedConversationOrFindsOwnedOne() {
        UUID userId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();
        String longTitle = "A".repeat(250);

        when(conversationRepository.save(any(ContentConversation.class))).thenAnswer(invocation -> {
            ContentConversation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", createdId);
            assertEquals(200, saved.getTitle().length());
            return saved;
        });

        assertEquals(createdId, service.getOrCreate(userId, null, longTitle));

        UUID existingId = UUID.randomUUID();
        ContentConversation existing = new ContentConversation(userId, "Title");
        ReflectionTestUtils.setField(existing, "id", existingId);
        when(conversationRepository.findByIdAndUserId(existingId, userId)).thenReturn(Optional.of(existing));

        assertEquals(existingId, service.getOrCreate(userId, existingId, "Ignored"));

        UUID foreignId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserId(foreignId, userId)).thenReturn(Optional.empty());
        ResponseStatusException notFound = assertThrows(ResponseStatusException.class,
                () -> service.getOrCreate(userId, foreignId, "Title"));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
    }

    @Test
    void recordExchangeListAndMessagesWorkForOwnedConversation() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        ContentConversation conversation = new ContentConversation(userId, "Campanha Q4");
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        ReflectionTestUtils.setField(conversation, "createdAt", Instant.now());
        ReflectionTestUtils.setField(conversation, "updatedAt", Instant.now());

        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(conversation));

        var request = new ContentProductionRequest(
                "Gerar leads", "Gestores", "LinkedIn", "Post", "Produto X", null, null, null, null, conversationId);
        service.recordExchange(userId, conversationId, request, "Rascunho gerado");

        verify(messageRepository).saveAll(any());
        verify(conversationRepository).save(conversation);
        assertNotNull(conversation.getUpdatedAt());

        assertEquals(1, service.list(userId).size());

        ContentConversationMessage msg = new ContentConversationMessage(conversationId, "ASSISTANT", "Rascunho gerado");
        ReflectionTestUtils.setField(msg, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(msg, "createdAt", Instant.now());
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId)).thenReturn(List.of(msg));

        var messages = service.messages(userId, conversationId);
        assertEquals(1, messages.size());
        assertEquals("ASSISTANT", messages.getFirst().role());
    }
}

