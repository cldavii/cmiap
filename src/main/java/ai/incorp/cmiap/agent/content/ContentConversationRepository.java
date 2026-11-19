package ai.incorp.cmiap.agent.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentConversationRepository extends JpaRepository<ContentConversation, UUID> {

    Optional<ContentConversation> findByIdAndUserId(UUID id, UUID userId);

    List<ContentConversation> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);
}
