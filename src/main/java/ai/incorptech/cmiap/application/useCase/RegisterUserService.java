package ai.incorptech.cmiap.application.useCase;

import ai.incorptech.cmiap.domain.model.User;
import ai.incorptech.cmiap.domain.model.UserRole;
import ai.incorptech.cmiap.domain.repository.UserRepository;
import ai.incorptech.cmiap.domain.valueObject.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User execute(String name, String rawEmail, String rawPassword) {
        Email email = new Email(rawEmail);
        // Verifica se o e-mail já está cadastrado
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("E-mail '" + u.getEmail() + "' já está em uso.");
        });
        // Criptografa a senha antes de salvar
        String hashedPassword = passwordEncoder.encode(rawPassword);
        // Cria a entidade de domínio com perfil padrão de usuário comum
        User newUser = new User(UUID.randomUUID(), name, email, hashedPassword, UserRole.ROLE_USER);
        return userRepository.save(newUser);
    }
}
