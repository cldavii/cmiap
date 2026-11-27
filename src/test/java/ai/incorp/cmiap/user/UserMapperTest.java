package ai.incorp.cmiap.user;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    void mapsRegistrationDtoToEntityWithDefaultRole() {
        var dto = new UserRegistrationDto("Ana", "Silva", 52998224725L, "ana@example.com", "secret", UserDepartment.COMMERCIAL);

        User entity = mapper.toEntity(dto);

        assertNull(entity.getId());
        assertEquals("Ana", entity.getName());
        assertEquals("Silva", entity.getSurname());
        assertEquals(52998224725L, entity.getCpf());
        assertEquals("ana@example.com", entity.getEmail());
        assertEquals("secret", entity.getPassword());
        assertEquals(UserRole.DEFAULT, entity.getRole());
        assertEquals(UserDepartment.COMMERCIAL, entity.getDepartment());
    }

    @Test
    void mapsEntityToResponseDto() {
        UUID id = UUID.randomUUID();
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setName("Ana");
        user.setSurname("Silva");
        user.setCpf(52998224725L);
        user.setEmail("ana@example.com");
        user.setRole(UserRole.ADMINISTRATOR);
        user.setDepartment(UserDepartment.MARKETING);

        UserResponseDto dto = mapper.toResponseDto(user);

        assertEquals(id, dto.id());
        assertEquals("Ana", dto.name());
        assertEquals("Silva", dto.surname());
        assertEquals(52998224725L, dto.cpf());
        assertEquals("ana@example.com", dto.email());
        assertEquals(UserRole.ADMINISTRATOR, dto.role());
        assertEquals(UserDepartment.MARKETING, dto.department());
    }
}
