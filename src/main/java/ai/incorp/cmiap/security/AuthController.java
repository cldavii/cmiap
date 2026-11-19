package ai.incorp.cmiap.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.security.cookies.secure:true}")
    private boolean secureCookies;

    @Value("${app.security.cookies.same-site:Lax}")
    private String sameSite;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void login(@RequestBody AuthRequest request, HttpServletResponse response) {
        setTokenCookies(response, authService.login(request));
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        setTokenCookies(response, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        clearCookie(response, "ACCESS_TOKEN", "/");
        clearCookie(response, "REFRESH_TOKEN", "/auth");
    }

    @GetMapping("/me")
    public AuthUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(jwt.getSubject());
    }

    private void setTokenCookies(HttpServletResponse response, AuthService.TokenPair tokens) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("ACCESS_TOKEN", tokens.accessToken())
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path("/")
                .maxAge(tokens.accessExpiresAt().getEpochSecond() - java.time.Instant.now().getEpochSecond())
                .build()
                .toString());
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("REFRESH_TOKEN", tokens.refreshToken())
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path("/auth")
                .maxAge(tokens.refreshTokenTtl())
                .build()
                .toString());
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0)
                .build()
                .toString());
    }
}
