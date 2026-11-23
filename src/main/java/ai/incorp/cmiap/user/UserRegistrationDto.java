package ai.incorp.cmiap.user;

public record UserRegistrationDto(
        String name,
        String surname,
        String cpf,
        String email,
        String password,
        UserDepartment department
) {}
