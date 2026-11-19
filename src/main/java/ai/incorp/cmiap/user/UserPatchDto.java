package ai.incorp.cmiap.user;

public record UserPatchDto(
        String name,
        String surname,
        Long cpf,
        String email,
        UserDepartment department
) {
}
