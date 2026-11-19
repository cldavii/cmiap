package ai.incorp.cmiap.agent.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentConversationMessageRepository extends JpaRepository<ContentConversationMessage, UUID> {

    List<ContentConversationMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
