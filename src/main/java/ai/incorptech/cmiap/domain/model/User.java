package ai.incorptech.cmiap.domain.model;

import ai.incorptech.cmiap.domain.valueObject.Email;
import lombok.Getter;

import java.util.UUID;

@Getter
public class User {

    private final UUID id;
    private final String name;
    private final Email email;
    private String password;
    private final UserRole role;

    public User(UUID id, String name, Email email, String password, UserRole role) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : UserRole.ROLE_USER;
    }

    public void updatePassword(String newHashedPassword) {
        this.password = newHashedPassword;
    }
}
