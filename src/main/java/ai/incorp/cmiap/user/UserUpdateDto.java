package ai.incorp.cmiap.user;

public record UserUpdateDto(
        String name,
        String surname,
        Long cpf,
        String email
) {
}
