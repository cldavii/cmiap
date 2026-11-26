package ai.incorp.cmiap.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final Long VALID_CPF = 52998224725L;
    private static final Long OTHER_VALID_CPF = 11144477735L;

    private UserRepository userRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, userMapper, passwordEncoder);
    }

    @Test
    void validatesCpfAndEmailFormats() {
        assertTrue(userService.isValidCpf(VALID_CPF));
        assertFalse(userService.isValidCpf(null));
        assertFalse(userService.isValidCpf(11111111111L));
        assertFalse(userService.isValidCpf(1234567890L));
        assertFalse(userService.isValidCpf(52998224720L));

        assertTrue(userService.isValidEmail("user@example.com"));
        assertFalse(userService.isValidEmail(null));
        assertFalse(userService.isValidEmail("invalid-email"));
        assertFalse(userService.isValidEmail("a".repeat(246) + "@test.com"));
    }

    @Test
    void registerUserNormalizesEmailEncodesPasswordAndSaves() {
        var dto = new UserRegistrationDto("Ana", "Silva", VALID_CPF, " Ana@Example.COM ", "secret123", UserDepartment.MARKETING);
        var mapped = createUser(UUID.randomUUID(), "Ana", "Silva", VALID_CPF, "Ana@Example.COM", UserRole.DEFAULT, UserDepartment.MARKETING);
        when(userRepository.existsByCpf(VALID_CPF)).thenReturn(false);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(mapped);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(mapped)).thenReturn(mapped);

        User saved = userService.registerUser(dto);

        assertEquals("ana@example.com", saved.getEmail());
        assertEquals("encoded-secret", saved.getPassword());
        verify(userRepository).save(mapped);
    }

    @Test
    void registerUserRejectsInvalidInputsAndConflicts() {
        assertThrows(UserValidationException.class, () -> userService.registerUser(null));
        assertThrows(UserValidationException.class, () -> userService.registerUser(
                new UserRegistrationDto("Ana", "Silva", 11111111111L, "ana@example.com", "secret", UserDepartment.MARKETING)));
        assertThrows(UserValidationException.class, () -> userService.registerUser(
                new UserRegistrationDto("Ana", "Silva", VALID_CPF, "invalid", "secret", UserDepartment.MARKETING)));
        assertThrows(UserValidationException.class, () -> userService.registerUser(
                new UserRegistrationDto(" ", "Silva", VALID_CPF, "ana@example.com", "secret", UserDepartment.MARKETING)));
        assertThrows(UserValidationException.class, () -> userService.registerUser(
                new UserRegistrationDto("Ana", "Silva", VALID_CPF, "ana@example.com", "secret", null)));

        var dto = new UserRegistrationDto("Ana", "Silva", VALID_CPF, "ana@example.com", "secret", UserDepartment.MARKETING);
        when(userRepository.existsByCpf(VALID_CPF)).thenReturn(true);
        assertThrows(UserConflictException.class, () -> userService.registerUser(dto));

        when(userRepository.existsByCpf(VALID_CPF)).thenReturn(false);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);
        assertThrows(UserConflictException.class, () -> userService.registerUser(dto));
    }

    @Test
    void provisionAdministratorHandlesNewExistingAndConflictCases() {
        var dto = new UserRegistrationDto("Admin", "Root", VALID_CPF, "Admin@Example.com", "strong-password-123", UserDepartment.MARKETING);
        var existingAdmin = createUser(UUID.randomUUID(), "Admin", "Root", VALID_CPF, "admin@example.com", UserRole.ADMINISTRATOR, UserDepartment.MARKETING);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(existingAdmin));

        assertSame(existingAdmin, userService.provisionAdministrator(dto));
        verify(userRepository, never()).save(any());

        var existingDefault = createUser(UUID.randomUUID(), "User", "Normal", VALID_CPF, "admin@example.com", UserRole.DEFAULT, UserDepartment.MARKETING);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(existingDefault));
        assertThrows(UserConflictException.class, () -> userService.provisionAdministrator(dto));

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByCpf(VALID_CPF)).thenReturn(true);
        assertThrows(UserConflictException.class, () -> userService.provisionAdministrator(dto));

        when(userRepository.existsByCpf(VALID_CPF)).thenReturn(false);
        var newAdmin = createUser(UUID.randomUUID(), "Admin", "Root", VALID_CPF, "admin@example.com", UserRole.DEFAULT, UserDepartment.MARKETING);
        when(userMapper.toEntity(dto)).thenReturn(newAdmin);
        when(passwordEncoder.encode("strong-password-123")).thenReturn("hashed-admin");
        when(userRepository.save(newAdmin)).thenReturn(newAdmin);

        User saved = userService.provisionAdministrator(dto);
        assertEquals(UserRole.ADMINISTRATOR, saved.getRole());
        assertEquals("hashed-admin", saved.getPassword());

        assertThrows(UserValidationException.class, () -> userService.provisionAdministrator(
                new UserRegistrationDto("Admin", "Root", VALID_CPF, "admin@example.com", "short", UserDepartment.MARKETING)));
    }

    @Test
    void lookupIdentityAndCredentialQueriesWork() {
        UUID id = UUID.randomUUID();
        var user = createUser(id, "Ana", "Silva", VALID_CPF, "ana@example.com", UserRole.DEFAULT, UserDepartment.COMMERCIAL);
        user.setPassword("hashed");

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByNameIgnoreCase("Ana")).thenReturn(List.of(user));
        when(userRepository.findByCpf(VALID_CPF)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));

        assertEquals(1, userService.listUsers().size());
        assertSame(user, userService.findUserById(id));
        assertEquals(1, userService.findUsersByName("Ana").size());
        assertSame(user, userService.findUserByCpf(VALID_CPF));
        assertSame(user, userService.findUserByEmail(" ana@example.com "));
        assertEquals("ana@example.com", userService.findIdentityByEmail(" ANA@example.com ").orElseThrow().email());
        assertEquals(id, userService.findIdentityById(id).orElseThrow().id());
        assertEquals("hashed", userService.findCredentialsByEmail("ana@example.com").orElseThrow().passwordHash());

        assertTrue(userService.findIdentityByEmail(" ").isEmpty());
        assertTrue(userService.findIdentityById(null).isEmpty());
        assertTrue(userService.findCredentialsByEmail(null).isEmpty());

        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.findUserById(missingId));
    }

    @Test
    void updatePatchAndDeleteUserWork() {
        UUID id = UUID.randomUUID();
        var user = createUser(id, "Ana", "Silva", VALID_CPF, "ana@example.com", UserRole.DEFAULT, UserDepartment.MARKETING);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByCpfAndIdNot(OTHER_VALID_CPF, id)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("novo@example.com", id)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateUser(id, new UserUpdateDto(" Bia ", " Costa ", OTHER_VALID_CPF, " Novo@Example.com "));
        assertEquals("Bia", updated.getName());
        assertEquals("Costa", updated.getSurname());
        assertEquals(OTHER_VALID_CPF, updated.getCpf());
        assertEquals("novo@example.com", updated.getEmail());

        User patched = userService.patchUser(id, new UserPatchDto(" Carla ", null, null, null));
        assertEquals("Carla", patched.getName());

        assertThrows(UserValidationException.class, () -> userService.patchUser(id, new UserPatchDto(null, null, null, null)));

        when(userRepository.existsByCpfAndIdNot(VALID_CPF, id)).thenReturn(true);
        assertThrows(UserConflictException.class, () -> userService.patchUser(id, new UserPatchDto(null, null, VALID_CPF, null)));

        userService.deleteUser(id);
        verify(userRepository).delete(user);
    }

    private static User createUser(
            UUID id,
            String name,
            String surname,
            Long cpf,
            String email,
            UserRole role,
            UserDepartment department
    ) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setName(name);
        user.setSurname(surname);
        user.setCpf(cpf);
        user.setEmail(email);
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }
}
