package ai.incorp.cmiap.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminProvisioningRunnerTest {

    @Test
    void failsWhenRequiredPropertiesAreMissingOrCpfIsInvalid() {
        UserService userService = mock(UserService.class);
        AdminProvisioningRunner runner = new AdminProvisioningRunner(userService);
        ReflectionTestUtils.setField(runner, "name", "");
        ReflectionTestUtils.setField(runner, "surname", "Root");
        ReflectionTestUtils.setField(runner, "cpf", "52998224725");
        ReflectionTestUtils.setField(runner, "email", "admin@example.com");
        ReflectionTestUtils.setField(runner, "password", "strong-password-123");

        assertThrows(IllegalStateException.class, () -> runner.run(new DefaultApplicationArguments()));

        ReflectionTestUtils.setField(runner, "name", "Admin");
        ReflectionTestUtils.setField(runner, "cpf", "not-a-number");
        assertThrows(IllegalStateException.class, () -> runner.run(new DefaultApplicationArguments()));
    }

    @Test
    void provisionsAdministratorAndVerifiesRole() {
        UserService userService = mock(UserService.class);
        AdminProvisioningRunner runner = new AdminProvisioningRunner(userService);
        ReflectionTestUtils.setField(runner, "name", "Admin");
        ReflectionTestUtils.setField(runner, "surname", "Root");
        ReflectionTestUtils.setField(runner, "cpf", "52998224725");
        ReflectionTestUtils.setField(runner, "email", "admin@example.com");
        ReflectionTestUtils.setField(runner, "password", "strong-password-123");

        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMINISTRATOR);
        when(userService.provisionAdministrator(any())).thenReturn(admin);

        runner.run(new DefaultApplicationArguments());
        verify(userService).provisionAdministrator(
                new UserRegistrationDto("Admin", "Root", 52998224725L, "admin@example.com", "strong-password-123", UserDepartment.MARKETING)
        );

        admin.setRole(UserRole.DEFAULT);
        assertThrows(IllegalStateException.class, () -> runner.run(new DefaultApplicationArguments()));
    }
}

