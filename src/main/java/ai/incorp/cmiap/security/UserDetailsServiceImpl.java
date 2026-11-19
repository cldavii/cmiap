package ai.incorp.cmiap.security;

import ai.incorp.cmiap.user.UserCredentials;
import ai.incorp.cmiap.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserCredentials user = userService.findCredentialsByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.email())
                .password(user.passwordHash())
                .roles(user.role().name())
                .build();
    }
}
