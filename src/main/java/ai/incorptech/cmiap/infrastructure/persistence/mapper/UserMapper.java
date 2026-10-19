package ai.incorptech.cmiap.infrastructure.persistence.mapper;

import ai.incorptech.cmiap.domain.model.User;
import ai.incorptech.cmiap.domain.valueObject.Email;
import ai.incorptech.cmiap.infrastructure.persistence.UserSetup;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toDomain(UserSetup entity);
    UserSetup toEntity(User domain);

    default Email map(String value) {
        return value != null ? new Email(value) : null;
    }

    default String map(Email email) {
        return email != null ? email.getValue() : null;
    }
}
