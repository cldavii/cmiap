package ai.incorp.cmiap.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@"
                    + "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public boolean isValidCpf(Long cpf) {
        if (cpf == null) {
            return false;
        }

        String digits = cpf.toString();
        if (digits.length() != 11 || digits.chars().allMatch(character -> character == digits.charAt(0))) {
            return false;
        }

        int firstCheckDigit = calculateCpfCheckDigit(digits, 9);
        int secondCheckDigit = calculateCpfCheckDigit(digits, 10);
        return digits.charAt(9) - '0' == firstCheckDigit
                && digits.charAt(10) - '0' == secondCheckDigit;
    }

    public boolean isValidEmail(String email) {
        return email != null
                && email.length() <= 254
                && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {
        if (registrationDto == null) {
            throw new UserValidationException("Os dados de cadastro são obrigatórios.");
        }
        if (!isValidCpf(registrationDto.cpf())) {
            throw new UserValidationException("CPF inválido.");
        }
        if (!isValidEmail(registrationDto.email())) {
            throw new UserValidationException("E-mail inválido.");
        }
        if (registrationDto.name() == null || registrationDto.name().isBlank()
                || registrationDto.surname() == null || registrationDto.surname().isBlank()
                || registrationDto.password() == null || registrationDto.password().isBlank()) {
            throw new UserValidationException("Nome, sobrenome e senha são obrigatórios.");
        }
        if (registrationDto.department() == null) {
            throw new UserValidationException("Selecione seu departamento.");
        }

        String normalizedEmail = registrationDto.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByCpf(registrationDto.cpf())) {
            throw new UserConflictException("Já existe um usuário cadastrado com este CPF.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserConflictException("Já existe um usuário cadastrado com este e-mail.");
        }

        User user = userMapper.toEntity(registrationDto);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(registrationDto.password()));
        return userRepository.save(user);
    }

    @Transactional
    public User provisionAdministrator(UserRegistrationDto registrationDto) {
        if (registrationDto == null) {
            throw new UserValidationException("Os dados do administrador são obrigatórios.");
        }
        validateName(registrationDto.name(), "Nome");
        validateName(registrationDto.surname(), "Sobrenome");
        validateCpf(registrationDto.cpf());
        validateEmail(registrationDto.email());
        validateAdministratorPassword(registrationDto.password());

        String normalizedEmail = normalizeEmail(registrationDto.email());
        var existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingUser.isPresent()) {
            if (existingUser.get().getRole() == UserRole.ADMINISTRATOR) {
                return existingUser.get();
            }
            throw new UserConflictException("O e-mail informado já pertence a uma conta não administradora.");
        }
        if (userRepository.existsByCpf(registrationDto.cpf())) {
            throw new UserConflictException("Já existe um usuário cadastrado com este CPF.");
        }

        User administrator = userMapper.toEntity(registrationDto);
        administrator.setEmail(normalizedEmail);
        administrator.setPassword(passwordEncoder.encode(registrationDto.password()));
        administrator.setRole(UserRole.ADMINISTRATOR);
        return userRepository.save(administrator);
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByName(String name) {
        return userRepository.findByNameIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public User findUserByCpf(Long cpf) {
        return userRepository.findByCpf(cpf)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public Optional<UserIdentity> findIdentityByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .map(UserService::toIdentity);
    }

    @Transactional(readOnly = true)
    public Optional<UserIdentity> findIdentityById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findById(id).map(UserService::toIdentity);
    }

    @Transactional(readOnly = true)
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .map(user -> new UserCredentials(user.getEmail(), user.getPassword(), user.getRole()));
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = findUserById(id);
        userRepository.delete(user);
    }

    @Transactional
    public User updateUser(UUID id, UserUpdateDto updateDto) {
        if (updateDto == null) {
            throw new UserValidationException("Os dados para atualização são obrigatórios.");
        }
        validateName(updateDto.name(), "Nome");
        validateName(updateDto.surname(), "Sobrenome");
        validateCpf(updateDto.cpf());
        validateEmail(updateDto.email());

        User user = findUserById(id);
        ensureCpfIsAvailable(updateDto.cpf(), id);
        String normalizedEmail = normalizeEmail(updateDto.email());
        ensureEmailIsAvailable(normalizedEmail, id);

        user.setName(updateDto.name().trim());
        user.setSurname(updateDto.surname().trim());
        user.setCpf(updateDto.cpf());
        user.setEmail(normalizedEmail);
        return userRepository.save(user);
    }

    @Transactional
    public User patchUser(UUID id, UserPatchDto patchDto) {
        if (patchDto == null || patchDto.name() == null && patchDto.surname() == null
                && patchDto.cpf() == null && patchDto.email() == null) {
            throw new UserValidationException("Informe ao menos um campo para atualização.");
        }

        User user = findUserById(id);

        if (patchDto.name() != null) {
            validateName(patchDto.name(), "Nome");
            user.setName(patchDto.name().trim());
        }
        if (patchDto.surname() != null) {
            validateName(patchDto.surname(), "Sobrenome");
            user.setSurname(patchDto.surname().trim());
        }
        if (patchDto.cpf() != null) {
            validateCpf(patchDto.cpf());
            ensureCpfIsAvailable(patchDto.cpf(), id);
            user.setCpf(patchDto.cpf());
        }
        if (patchDto.email() != null) {
            validateEmail(patchDto.email());
            String normalizedEmail = normalizeEmail(patchDto.email());
            ensureEmailIsAvailable(normalizedEmail, id);
            user.setEmail(normalizedEmail);
        }

        return userRepository.save(user);
    }

    private void validateName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new UserValidationException(fieldName + " é obrigatório.");
        }
    }

    private void validateCpf(Long cpf) {
        if (!isValidCpf(cpf)) {
            throw new UserValidationException("CPF inválido.");
        }
    }

    private void validateEmail(String email) {
        if (!isValidEmail(email)) {
            throw new UserValidationException("E-mail inválido.");
        }
    }

    private void validateAdministratorPassword(String password) {
        if (password == null
                || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new UserValidationException(
                    "A senha do administrador deve ter ao menos 12 caracteres e no máximo 72 bytes UTF-8."
            );
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserIdentity toIdentity(User user) {
        return new UserIdentity(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment()
        );
    }

    private void ensureCpfIsAvailable(Long cpf, UUID userId) {
        if (userRepository.existsByCpfAndIdNot(cpf, userId)) {
            throw new UserConflictException("Já existe um usuário cadastrado com este CPF.");
        }
    }

    private void ensureEmailIsAvailable(String email, UUID userId) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new UserConflictException("Já existe um usuário cadastrado com este e-mail.");
        }
    }

    private int calculateCpfCheckDigit(String digits, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (digits.charAt(index) - '0') * (length + 1 - index);
        }

        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}
