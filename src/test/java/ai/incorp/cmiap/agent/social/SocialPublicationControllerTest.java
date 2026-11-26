package ai.incorp.cmiap.agent.social;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SocialPublicationControllerTest {

    private final SocialPublicationService service = mock(SocialPublicationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SocialPublicationController(service))
                .setValidator(validator)
                .build();
    }

    @Test
    void postPublicationPlanReturns200AndRejectsInvalidBody() throws Exception {
        OffsetDateTime plannedAt = OffsetDateTime.of(2026, 12, 1, 14, 0, 0, 0, ZoneOffset.UTC);
        var rules = new SocialPublicationRequestDTO.TechnicalRules(
                "ig-post-v1", 2200, false, List.of("image/png"), null, null, null);
        var request = new SocialPublicationRequestDTO(
                SocialPublicationRequestDTO.Platform.INSTAGRAM,
                SocialPublicationRequestDTO.Format.POST,
                "content-1",
                "Legenda aprovada",
                null,
                rules,
                "v2",
                "v2",
                plannedAt,
                List.of(plannedAt),
                List.of(),
                60,
                List.of(),
                true
        );

        var response = new SocialPublicationResponseDTO(
                SocialPublicationResponseDTO.Status.SIMULATED,
                SocialPublicationRequestDTO.Platform.INSTAGRAM,
                SocialPublicationRequestDTO.Format.POST,
                plannedAt,
                List.of(),
                "ig-post-v1",
                new SocialPublicationResponseDTO.Simulation("sim-1", "content-1", "v2"),
                List.of()
        );
        when(service.check(any())).thenReturn(response);

        mockMvc.perform(post("/agents/social/publication-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIMULATED"))
                .andExpect(jsonPath("$.simulation.reference").value("sim-1"));

        mockMvc.perform(post("/agents/social/publication-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

