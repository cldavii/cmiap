package ai.incorp.cmiap.user;

public record UserCredentials(String email, String passwordHash, UserRole role) {
}
