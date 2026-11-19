package ai.incorp.cmiap.security;

public record AuthRequest(String email, String password, AccessProfile department) {
    public enum AccessProfile {
        MARKETING,
        COMMERCIAL,
        ADMINISTRATOR
    }
}
