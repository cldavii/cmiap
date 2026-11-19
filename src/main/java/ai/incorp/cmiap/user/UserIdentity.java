package ai.incorp.cmiap.user;

import java.util.UUID;

public record UserIdentity(
        UUID id,
        String name,
        String surname,
        String email,
        UserRole role,
        UserDepartment department
) {
}
