package ai.incorptech.cmiap.domain.repository;

import ai.incorptech.cmiap.domain.model.User;
import ai.incorptech.cmiap.domain.valueObject.Email;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(Email email);
}
