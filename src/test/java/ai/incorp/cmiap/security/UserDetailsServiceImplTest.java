package ai.incorp.cmiap.security;

import ai.incorp.cmiap.user.UserCredentials;
import ai.incorp.cmiap.user.UserRole;
import ai.incorp.cmiap.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    @Test
    void loadsUserDetailsAndThrowsWhenMissing() {
        UserService userService = mock(UserService.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userService);

        when(userService.findCredentialsByEmail("ana@example.com"))
                .thenReturn(Optional.of(new UserCredentials("ana@example.com", "hashed", UserRole.ADMINISTRATOR)));

        UserDetails details = service.loadUserByUsername("ana@example.com");
        assertEquals("ana@example.com", details.getUsername());
        assertEquals("hashed", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMINISTRATOR".equals(a.getAuthority())));

        when(userService.findCredentialsByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@example.com"));
    }
}

