package ai.incorp.cmiap.agent.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContentProductionControllerTest {

    private final ContentProductionService service = mock(ContentProductionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();
    private Jwt activeJwt;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        activeJwt = jwtWithUserId(userId.toString());
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ContentProductionController(service))
                .setValidator(validator)
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
                        return activeJwt;
                    }
                })
                .build();
    }

    @Test
    void produceListConversationsAndMessagesWork() throws Exception {
        UUID conversationId = UUID.randomUUID();
        var request = new ContentProductionRequest(
                "Gerar demanda", "Diretores de TI", "LinkedIn", "Carrossel", "Fatos do produto", null, null, null, "pt-BR", null);
        when(service.produce(eq(userId), any())).thenReturn(new ContentProductionResponse(conversationId, "Draft", true));
        when(service.listConversations(userId)).thenReturn(List.of(
                new ContentConversationSummary(conversationId, "Gerar demanda", Instant.now(), Instant.now())));
        when(service.listMessages(userId, conversationId)).thenReturn(List.of(
                new ContentConversationMessageResponse(UUID.randomUUID(), "ASSISTANT", "Draft", Instant.now())));

        mockMvc.perform(post("/agents/content/produce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.humanReviewRequired").value(true));

        mockMvc.perform(get("/agents/content/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Gerar demanda"));

        mockMvc.perform(get("/agents/content/conversations/{id}/messages", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ASSISTANT"));
    }

    @Test
    void rejectsInvalidJwtUserIdClaimAndInvalidPayload() throws Exception {
        activeJwt = jwtWithUserId("not-a-uuid");
        mockMvc.perform(get("/agents/content/conversations"))
                .andExpect(status().isUnauthorized());

        activeJwt = jwtWithUserId(userId.toString());
        mockMvc.perform(post("/agents/content/produce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private static Jwt jwtWithUserId(String userIdClaim) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ana@example.com")
                .claim("user_id", userIdClaim)
                .build();
    }
}

