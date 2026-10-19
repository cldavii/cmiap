package ai.incorptech.cmiap.infrastructure.persistence;

import ai.incorptech.cmiap.domain.model.User;
import ai.incorptech.cmiap.domain.repository.UserRepository;
import ai.incorptech.cmiap.domain.valueObject.Email;
import ai.incorptech.cmiap.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserRepositorySetup userRepositorySetup;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        UserSetup entity = userMapper.toEntity(user);
        UserSetup savedEntity = userRepositorySetup.save(entity);
        return userMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userRepositorySetup.findByEmail(email.getValue())
                .map(userMapper::toDomain);
    }
}
