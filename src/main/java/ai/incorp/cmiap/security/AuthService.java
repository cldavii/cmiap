package ai.incorp.cmiap.security;

import ai.incorp.cmiap.user.UserIdentity;
import ai.incorp.cmiap.user.UserDepartment;
import ai.incorp.cmiap.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtEncoder jwtEncoder;

    @Value("${app.security.jwt.access-token-ttl:PT10M}")
    private Duration accessTokenTtl;

    @Value("${app.security.jwt.refresh-token-ttl:P7D}")
    private Duration refreshTokenTtl;

    @Transactional
    public TokenPair login(AuthRequest request) {
        if (request == null || request.email() == null || request.password() == null || request.department() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail, senha e departamento são obrigatórios.");
        }

        String email = request.email().trim();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.", exception);
        }

        UserIdentity user = userService.findIdentityByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));
        if (user.department() != request.department()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciais ou departamento incorretos."
            );
        }
        return createTokenPair(user);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token ausente ou inválido.");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenHashForUpdate(hashToken(rawRefreshToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token ausente ou inválido."));
        Instant now = Instant.now();
        if (storedToken.getRevokedAt() != null || !storedToken.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado ou revogado.");
        }

        storedToken.setRevokedAt(now);
        UserIdentity user = userService.findIdentityById(storedToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));
        return createTokenPair(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(hashToken(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        return userService.findIdentityByEmail(email)
                .map(AuthUserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));
    }

    private TokenPair createTokenPair(UserIdentity user) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(accessTokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cmiap")
                .issuedAt(now)
                .expiresAt(accessExpiresAt)
                .subject(user.email())
                .claim("user_id", user.id().toString())
                .claim("role", user.role().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String refreshTokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        refreshTokenRepository.save(new RefreshToken(
                user.id(),
                hashToken(refreshTokenValue),
                now.plus(refreshTokenTtl)
        ));

        return new TokenPair(accessToken, refreshTokenValue, accessExpiresAt, refreshTokenTtl);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Instant accessExpiresAt,
            Duration refreshTokenTtl
    ) {
    }
}
