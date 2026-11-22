package ai.incorp.cmiap.user;

public record UserUpdateDto(
        String name,
        String surname,
        String cpf,
        String email
) {
}
