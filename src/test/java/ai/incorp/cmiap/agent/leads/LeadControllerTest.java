package ai.incorp.cmiap.agent.leads;

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

class LeadControllerTest {

    private final LeadService leadService = mock(LeadService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();
    private Jwt activeJwt;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        activeJwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ana@example.com")
                .claim("user_id", userId.toString())
                .build();

        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new LeadController(leadService))
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
    void listCreateImportAndQualifyEndpointsWork() throws Exception {
        UUID leadId = UUID.randomUUID();
        LeadResponse response = new LeadResponse(
                leadId, "João", "Acme", "CTO", "joao@acme.com", null, null,
                "SaaS", "50-200", "SP", null, null, null, null, null, null, Instant.now(), Instant.now());

        when(leadService.list(userId)).thenReturn(List.of(response));
        when(leadService.create(eq(userId), any())).thenReturn(response);
        when(leadService.importLeads(eq(userId), any())).thenReturn(List.of(response));
        when(leadService.qualify(eq(userId), eq(leadId), any())).thenReturn(
                new LeadQualificationResult(leadId, 88, LeadFit.QUALIFIED, "Bom fit", "Agendar demo", Instant.now()));

        var leadRequest = new LeadRequest("João", "Acme", "CTO", "joao@acme.com", null, null, "SaaS", "50-200", "SP", null);

        mockMvc.perform(get("/agents/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("João"));

        mockMvc.perform(post("/agents/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(leadId.toString()));

        mockMvc.perform(post("/agents/leads/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeadImportRequest(List.of(leadRequest)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].email").value("joao@acme.com"));

        mockMvc.perform(post("/agents/leads/{leadId}/qualify", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeadQualificationRequest("Empresas B2B SaaS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(88))
                .andExpect(jsonPath("$.fit").value("QUALIFIED"));
    }

    @Test
    void rejectsInvalidTokenAndBlankLeadName() throws Exception {
        activeJwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("ana@example.com").build();
        mockMvc.perform(get("/agents/leads")).andExpect(status().isUnauthorized());

        activeJwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ana@example.com")
                .claim("user_id", userId.toString())
                .build();
        mockMvc.perform(post("/agents/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
