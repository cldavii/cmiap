package ai.incorp.cmiap;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import ai.incorp.cmiap.security.RefreshTokenRepository;
import ai.incorp.cmiap.user.User;
import ai.incorp.cmiap.user.UserRegistrationDto;
import ai.incorp.cmiap.user.UserRepository;
import ai.incorp.cmiap.user.UserService;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigurationTests {

    private static final String TEST_JWT_SECRET = createTestJwtSecret();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @DynamicPropertySource
    static void registerSecurityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("langchain4j.google-ai-gemini.chat-model.api-key", () -> "test-gemini-api-key");
        registry.add("langchain4j.google-ai-gemini.chat-model.model-name", () -> "gemini-2.5-flash");
    }

    @Test
    void csrfEndpointReturnsTokenForPostmanAndBrowserClients() throws Exception {
        mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @Transactional
    void currentUserReturnsSafeProfileFromAccessTokenCookie() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        User user = userService.registerUser(new UserRegistrationDto(
                "Test", "User", validCpf(), email, "Test-password-123"
        ));

        try {
            mockMvc.perform(get("/auth/me")
                            .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole(
                                    "DEFAULT", user.getId(), email
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId().toString()))
                    .andExpect(jsonPath("$.name").value("Test"))
                    .andExpect(jsonPath("$.surname").value("User"))
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.role").value("DEFAULT"))
                    .andExpect(jsonPath("$.cpf").doesNotExist())
                    .andExpect(jsonPath("$.password").doesNotExist());
        } finally {
            userRepository.delete(user);
        }
    }

    @Test
    void corsRejectsOriginsUntilOneIsConfigured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.options("/auth/login")
                        .header("Origin", "https://frontend.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void userManagementRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contentProductionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/agents/content/produce")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leadManagementRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/agents/leads"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/agents/leads")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leadEndpointsValidateInput() throws Exception {
        mockMvc.perform(post("/agents/leads")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole("DEFAULT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/agents/leads/import")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole("DEFAULT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leads\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void leadCanBeCreatedAndIsVisibleOnlyToItsOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = accessTokenWithRole("DEFAULT", userId);
        mockMvc.perform(post("/agents/leads")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Marina Costa","company":"Acme","industry":"Tecnologia"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Marina Costa"))
                .andExpect(jsonPath("$.company").value("Acme"))
                .andExpect(jsonPath("$.score").doesNotExist());

        mockMvc.perform(get("/agents/leads")
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Marina Costa"));

        mockMvc.perform(get("/agents/leads")
                        .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole("DEFAULT", UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    void csvLeadBatchCanBeImported() throws Exception {
        mockMvc.perform(post("/agents/leads/import")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole("DEFAULT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leads":[
                                  {"name":"Ana Souza","company":"Empresa A"},
                                  {"name":"Bruno Lima","company":"Empresa B"}
                                ]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ana Souza"))
                .andExpect(jsonPath("$[1].name").value("Bruno Lima"));
    }

    @Test
    void contentProductionValidatesRequiredInput() throws Exception {
        mockMvc.perform(post("/agents/content/produce")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole("DEFAULT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contentConversationMessagesAreScopedToTheAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        mockMvc.perform(get("/agents/content/conversations")
                        .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole("DEFAULT", userId))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/agents/content/conversations/{conversationId}/messages", conversationId)
                        .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole("DEFAULT", userId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void userManagementRequiresAdministratorRole() throws Exception {
        mockMvc.perform(get("/users")
                        .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole("DEFAULT"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users")
                        .cookie(new Cookie("ACCESS_TOKEN", accessTokenWithRole("ADMINISTRATOR"))))
                .andExpect(status().isOk());
    }

    @Test
    void registrationIsPublicButStillRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/users/register")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .cookie(new Cookie("ACCESS_TOKEN", "expired-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void csrfTokenCanBeFetchedAndReturnedOnTheNextRequest() throws Exception {
        CsrfDetails csrf = getCsrfDetails();

        mockMvc.perform(post("/users/register")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void loginRefreshAndLogoutManageHttpOnlyCookiesAndRefreshRevocation() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        User user = userService.registerUser(new UserRegistrationDto(
                "Test", "User", validCpf(), email, "Test-password-123"
        ));

        try {
            CsrfDetails csrf = getCsrfDetails();
            MvcResult loginResult = mockMvc.perform(post("/auth/login")
                            .session(csrf.session())
                            .header(csrf.headerName(), csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"Test-password-123\"}"))
                    .andExpect(status().isNoContent())
                    .andReturn();
            String firstRefreshToken = cookieValue(loginResult, "REFRESH_TOKEN");
            cookieValue(loginResult, "ACCESS_TOKEN");

            MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                            .session(csrf.session())
                            .header(csrf.headerName(), csrf.token())
                            .cookie(new Cookie("REFRESH_TOKEN", firstRefreshToken)))
                    .andExpect(status().isNoContent())
                    .andReturn();
            String rotatedRefreshToken = cookieValue(refreshResult, "REFRESH_TOKEN");
            org.junit.jupiter.api.Assertions.assertNotEquals(firstRefreshToken, rotatedRefreshToken);

            mockMvc.perform(post("/auth/logout")
                            .session(csrf.session())
                            .header(csrf.headerName(), csrf.token())
                            .cookie(new Cookie("REFRESH_TOKEN", rotatedRefreshToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/auth/refresh")
                            .session(csrf.session())
                            .header(csrf.headerName(), csrf.token())
                            .cookie(new Cookie("REFRESH_TOKEN", rotatedRefreshToken)))
                    .andExpect(status().isUnauthorized());
        } finally {
            refreshTokenRepository.deleteAllByUserId(user.getId());
            userRepository.delete(user);
        }
    }

    private CsrfDetails getCsrfDetails() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Matcher tokenMatcher = Pattern.compile("\"headerName\":\"([^\"]+)\".*\"token\":\"([^\"]+)\"")
                .matcher(csrfResult.getResponse().getContentAsString());
        assertTrue(tokenMatcher.find());
        return new CsrfDetails(
                (MockHttpSession) csrfResult.getRequest().getSession(false),
                tokenMatcher.group(1),
                tokenMatcher.group(2)
        );
    }

    private String cookieValue(MvcResult result, String cookieName) {
        String prefix = cookieName + "=";
        return result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(header -> header.startsWith(prefix))
                .map(header -> header.substring(prefix.length()).split(";", 2)[0])
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cookie " + cookieName + " was not set."));
    }

    private long validCpf() {
        String base = Long.toString(100_000_000L + new java.security.SecureRandom().nextInt(900_000_000));
        int first = cpfDigit(base, 9);
        int second = cpfDigit(base + first, 10);
        return Long.parseLong(base + first + second);
    }

    private int cpfDigit(String digits, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (digits.charAt(index) - '0') * (length + 1 - index);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private static String createTestJwtSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }

    private String accessTokenWithRole(String role) {
        return accessTokenWithRole(role, UUID.randomUUID(), "test@example.com");
    }

    private String accessTokenWithRole(String role, UUID userId) {
        return accessTokenWithRole(role, userId, "test@example.com");
    }

    private String accessTokenWithRole(String role, UUID userId, String email) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cmiap")
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("role", role)
                .claim("user_id", userId.toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }

    private record CsrfDetails(MockHttpSession session, String headerName, String token) {
    }
}
