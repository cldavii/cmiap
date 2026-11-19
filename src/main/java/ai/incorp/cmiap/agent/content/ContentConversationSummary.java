package ai.incorp.cmiap.agent.content;

import java.time.Instant;
import java.util.UUID;

public record ContentConversationSummary(UUID id, String title, Instant createdAt, Instant updatedAt) {

    static ContentConversationSummary from(ContentConversation conversation) {
        return new ContentConversationSummary(
                conversation.getId(), conversation.getTitle(), conversation.getCreatedAt(), conversation.getUpdatedAt()
        );
    }
}
