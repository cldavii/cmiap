package ai.incorp.cmiap.user;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String name,
        String surname,
        String cpf,
        String email,
        UserRole role,
        UserDepartment department
) {
}
