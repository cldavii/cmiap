package ai.incorp.cmiap.user;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String name,
        String surname,
        Long cpf,
        String email,
        UserRole role
) {
}
