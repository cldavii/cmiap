package ai.incorp.cmiap.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "DEFAULT")
    User toEntity(UserRegistrationDto registrationDto);

    UserResponseDto toResponseDto(User user);
}
