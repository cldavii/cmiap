package ai.incorp.cmiap.user;

public record UserRegistrationDto(
        String name,
        String surname,
        Long cpf,
        String email,
        String password
) {}
