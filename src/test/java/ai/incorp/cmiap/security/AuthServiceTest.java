package ai.incorp.cmiap.security;

import ai.incorp.cmiap.user.UserDepartment;
import ai.incorp.cmiap.user.UserIdentity;
import ai.incorp.cmiap.user.UserRole;
import ai.incorp.cmiap.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private UserService userService;
    private RefreshTokenRepository refreshTokenRepository;
    private JwtEncoder jwtEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        userService = mock(UserService.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        jwtEncoder = mock(JwtEncoder.class);
        authService = new AuthService(authenticationManager, userService, refreshTokenRepository, jwtEncoder);
        ReflectionTestUtils.setField(authService, "accessTokenTtl", Duration.ofMinutes(10));
        ReflectionTestUtils.setField(authService, "refreshTokenTtl", Duration.ofDays(7));
    }

    @Test
    void loginIssuesTokensWhenCredentialsAndDepartmentMatch() {
        UUID userId = UUID.randomUUID();
        var identity = new UserIdentity(userId, "Ana", "Silva", "ana@example.com", UserRole.DEFAULT, UserDepartment.MARKETING);
        Jwt encodedJwt = Jwt.withTokenValue("jwt-token")
                .header("alg", "HS256")
                .subject("ana@example.com")
                .build();

        when(userService.findIdentityByEmail("ana@example.com")).thenReturn(Optional.of(identity));
        when(jwtEncoder.encode(any())).thenReturn(encodedJwt);

        AuthService.TokenPair tokens = authService.login(new AuthRequest(" ana@example.com ", "secret", UserDepartment.MARKETING));

        assertEquals("jwt-token", tokens.accessToken());
        assertNotNull(tokens.refreshToken());
        assertEquals(Duration.ofDays(7), tokens.refreshTokenTtl());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginRejectsMissingFieldsBadCredentialsAndDepartmentMismatch() {
        ResponseStatusException badRequest = assertThrows(ResponseStatusException.class,
                () -> authService.login(new AuthRequest(null, "secret", UserDepartment.MARKETING)));
        assertEquals(HttpStatus.BAD_REQUEST, badRequest.getStatusCode());

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        ResponseStatusException badCreds = assertThrows(ResponseStatusException.class,
                () -> authService.login(new AuthRequest("ana@example.com", "wrong", UserDepartment.MARKETING)));
        assertEquals(HttpStatus.UNAUTHORIZED, badCreds.getStatusCode());

        AuthenticationManager passingManager = mock(AuthenticationManager.class);
        AuthService service = new AuthService(passingManager, userService, refreshTokenRepository, jwtEncoder);
        ReflectionTestUtils.setField(service, "accessTokenTtl", Duration.ofMinutes(10));
        ReflectionTestUtils.setField(service, "refreshTokenTtl", Duration.ofDays(7));

        var identity = new UserIdentity(UUID.randomUUID(), "Ana", "Silva", "ana@example.com", UserRole.DEFAULT, UserDepartment.COMMERCIAL);
        when(userService.findIdentityByEmail("ana@example.com")).thenReturn(Optional.of(identity));

        ResponseStatusException wrongDept = assertThrows(ResponseStatusException.class,
                () -> service.login(new AuthRequest("ana@example.com", "secret", UserDepartment.MARKETING)));
        assertEquals(HttpStatus.UNAUTHORIZED, wrongDept.getStatusCode());
    }

    @Test
    void refreshRotatesActiveTokenAndRejectsInvalidOrExpiredTokens() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> authService.refresh(" ")).getStatusCode());

        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> authService.refresh("missing")).getStatusCode());

        UUID userId = UUID.randomUUID();
        RefreshToken expired = new RefreshToken(userId, "hash", Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(expired));
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> authService.refresh("expired")).getStatusCode());

        RefreshToken active = new RefreshToken(userId, "hash", Instant.now().plusSeconds(3600));
        var identity = new UserIdentity(userId, "Ana", "Silva", "ana@example.com", UserRole.DEFAULT, UserDepartment.MARKETING);
        Jwt encodedJwt = Jwt.withTokenValue("new-jwt")
                .header("alg", "HS256")
                .subject("ana@example.com")
                .build();

        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(active));
        when(userService.findIdentityById(userId)).thenReturn(Optional.of(identity));
        when(jwtEncoder.encode(any())).thenReturn(encodedJwt);

        AuthService.TokenPair rotated = authService.refresh("raw-token");
        assertEquals("new-jwt", rotated.accessToken());
        assertNotNull(active.getRevokedAt());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void logoutRevokesActiveTokenAndCurrentUserReturnsProfile() {
        authService.logout(null);
        verify(refreshTokenRepository, never()).findByTokenHashForUpdate(any());

        UUID userId = UUID.randomUUID();
        RefreshToken active = new RefreshToken(userId, "hash", Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(active));

        authService.logout("raw-token");
        assertNotNull(active.getRevokedAt());

        var identity = new UserIdentity(userId, "Ana", "Silva", "ana@example.com", UserRole.ADMINISTRATOR, UserDepartment.MARKETING);
        when(userService.findIdentityByEmail("ana@example.com")).thenReturn(Optional.of(identity));

        AuthUserResponse profile = authService.currentUser("ana@example.com");
        assertEquals(userId, profile.id());
        assertEquals(UserRole.ADMINISTRATOR, profile.role());
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> authService.currentUser(" ")).getStatusCode());
    }
}
