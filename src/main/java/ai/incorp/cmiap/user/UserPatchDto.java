package ai.incorp.cmiap.user;

public record UserPatchDto(
        String name,
        String surname,
        String cpf,
        String email,
        UserDepartment department
) {
}
