package ai.incorp.cmiap.security;

import ai.incorp.cmiap.user.UserIdentity;
import ai.incorp.cmiap.user.UserRole;

import java.util.UUID;

public record AuthUserResponse(UUID id, String name, String surname, String email, UserRole role) {

    static AuthUserResponse from(UserIdentity user) {
        return new AuthUserResponse(user.id(), user.name(), user.surname(), user.email(), user.role());
    }
}
