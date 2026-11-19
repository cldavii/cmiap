package ai.incorp.cmiap.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByCpf(Long cpf);

    boolean existsByEmail(String email);

    boolean existsByCpfAndIdNot(Long cpf, UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    List<User> findByNameIgnoreCase(String name);

    Optional<User> findByCpf(Long cpf);

    Optional<User> findByEmailIgnoreCase(String email);
}
