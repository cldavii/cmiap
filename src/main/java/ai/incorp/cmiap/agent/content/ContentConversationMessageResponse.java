package ai.incorp.cmiap.agent.content;

import java.time.Instant;
import java.util.UUID;

public record ContentConversationMessageResponse(UUID id, String role, String content, Instant createdAt) {

    static ContentConversationMessageResponse from(ContentConversationMessage message) {
        return new ContentConversationMessageResponse(
                message.getId(), message.getRole(), message.getContent(), message.getCreatedAt()
        );
    }
}
