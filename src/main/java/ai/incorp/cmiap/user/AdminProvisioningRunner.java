package ai.incorp.cmiap.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.security.bootstrap-admin.enabled", havingValue = "true")
public class AdminProvisioningRunner implements ApplicationRunner {

    private final UserService userService;

    @Value("${app.security.bootstrap-admin.name:}")
    private String name;

    @Value("${app.security.bootstrap-admin.surname:}")
    private String surname;

    @Value("${app.security.bootstrap-admin.cpf:}")
    private String cpf;

    @Value("${app.security.bootstrap-admin.email:}")
    private String email;

    @Value("${app.security.bootstrap-admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (name.isBlank() || surname.isBlank() || cpf.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Defina ADMIN_NAME, ADMIN_SURNAME, ADMIN_CPF, ADMIN_EMAIL e ADMIN_PASSWORD para provisionar o administrador."
            );
        }

        long cpfNumber;
        try {
            cpfNumber = Long.parseLong(cpf);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("ADMIN_CPF deve conter somente os 11 dígitos do CPF.", exception);
        }

        User admin = userService.provisionAdministrator(
                new UserRegistrationDto(name, surname, cpfNumber, email, password, UserDepartment.MARKETING)
        );
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new IllegalStateException("O provisionamento não resultou em uma conta administradora.");
        }
        log.info("Conta administradora provisionada ou já existente para o e-mail: {}", admin.getEmail());
    }
}
