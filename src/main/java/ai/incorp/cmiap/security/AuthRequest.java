package ai.incorp.cmiap.security;

import ai.incorp.cmiap.user.UserDepartment;

public record AuthRequest(String email, String password, UserDepartment department) {
}
