package ai.incorptech.cmiap.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositorySetup extends JpaRepository<UserSetup, UUID> {
    Optional<UserSetup> findByEmail(String email);
}
