package ai.incorp.cmiap.security;

import ai.incorp.cmiap.user.UserDepartment;
import ai.incorp.cmiap.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        ReflectionTestUtils.setField(controller, "secureCookies", false);
        ReflectionTestUtils.setField(controller, "sameSite", "Lax");

        Jwt principal = Jwt.withTokenValue("access-jwt")
                .header("alg", "HS256")
                .subject("ana@example.com")
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(Jwt.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory
                    ) {
                        return principal;
                    }
                })
                .build();
    }

    @Test
    void loginRefreshLogoutAndMeEndpointsWork() throws Exception {
        var pair = new AuthService.TokenPair(
                "access-123",
                "refresh-123",
                Instant.now().plusSeconds(600),
                Duration.ofDays(7)
        );
        when(authService.login(any())).thenReturn(pair);
        when(authService.refresh("refresh-cookie")).thenReturn(pair);

        var loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest("ana@example.com", "secret", UserDepartment.MARKETING))))
                .andExpect(status().isNoContent())
                .andReturn();

        List<String> loginCookies = loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertTrue(loginCookies.stream().anyMatch(c -> c.startsWith("ACCESS_TOKEN=access-123")));
        assertTrue(loginCookies.stream().anyMatch(c -> c.startsWith("REFRESH_TOKEN=refresh-123")));

        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("REFRESH_TOKEN", "refresh-cookie")))
                .andExpect(status().isNoContent());

        var logoutResult = mockMvc.perform(post("/auth/logout").cookie(new Cookie("REFRESH_TOKEN", "refresh-cookie")))
                .andExpect(status().isNoContent())
                .andReturn();
        verify(authService).logout("refresh-cookie");
        List<String> logoutCookies = logoutResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertTrue(logoutCookies.stream().anyMatch(c -> c.contains("ACCESS_TOKEN=") && c.contains("Max-Age=0")));

        UUID userId = UUID.randomUUID();
        when(authService.currentUser("ana@example.com")).thenReturn(new AuthUserResponse(
                userId, "Ana", "Silva", "ana@example.com", UserRole.DEFAULT, UserDepartment.MARKETING));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.department").value("MARKETING"));
    }
}

