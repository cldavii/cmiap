package ai.incorp.cmiap.agent.content;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentConversationService {

    private final ContentConversationRepository conversationRepository;
    private final ContentConversationMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID getOrCreate(UUID userId, UUID conversationId, String title) {
        if (conversationId == null) {
            return conversationRepository.save(new ContentConversation(userId, truncateTitle(title))).getId();
        }
        return findOwnedConversation(userId, conversationId).getId();
    }

    @Transactional
    public void recordExchange(UUID userId, UUID conversationId, ContentProductionRequest request, String draft) {
        ContentConversation conversation = findOwnedConversation(userId, conversationId);
        try {
            String userContent = objectMapper.writeValueAsString(request);
            messageRepository.saveAll(List.of(
                    new ContentConversationMessage(conversationId, "USER", userContent),
                    new ContentConversationMessage(conversationId, "ASSISTANT", draft)
            ));
            conversation.setUpdatedAt(java.time.Instant.now());
            conversationRepository.save(conversation);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Não foi possível registrar a mensagem da conversa.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<ContentConversationSummary> list(UUID userId) {
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ContentConversationSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContentConversationMessageResponse> messages(UUID userId, UUID conversationId) {
        findOwnedConversation(userId, conversationId);
        return messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(ContentConversationMessageResponse::from)
                .toList();
    }

    private ContentConversation findOwnedConversation(UUID userId, UUID conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa não encontrada."));
    }

    private String truncateTitle(String value) {
        return value.length() <= 200 ? value : value.substring(0, 200);
    }
}
