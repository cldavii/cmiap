package ai.incorp.cmiap.agent.leads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    List<Lead> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<Lead> findByIdAndUserId(UUID id, UUID userId);
}
