package ai.incorptech.cmiap.application.useCase;

import ai.incorptech.cmiap.domain.model.User;
import ai.incorptech.cmiap.domain.repository.UserRepository;
import ai.incorptech.cmiap.domain.valueObject.Email;
import ai.incorptech.cmiap.infrastructure.security.provider.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticateUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticateUserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public String execute(String rawEmail, String rawPassword) {
        // 1. Valida e instancia o Value Object de E-mail
        Email email = new Email(rawEmail);

        // 2. Busca o usuário pelo e-mail utilizando o repositório de domínio
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));

        // 3. Compara a senha informada com o hash armazenado no banco
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        // 4. Se autenticado com sucesso, gera e retorna o Token JWT
        return jwtService.generateToken(user);
    }
}
