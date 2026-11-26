package ai.incorp.cmiap.agent.traffic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrafficCampaignControllerTest {

    private final TrafficCampaignService service = mock(TrafficCampaignService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TrafficCampaignController(service))
                .setValidator(validator)
                .build();
    }

    @Test
    void postCampaignPlanReturns200AndRejectsInvalidBody() throws Exception {
        var budget = new TrafficCampaignRequestDTO.Budget("BRL", new BigDecimal("1000.00"), new BigDecimal("1500.00"));
        var goals = new TrafficCampaignRequestDTO.Goals(new BigDecimal("50.00"), new BigDecimal("3.00"));
        var creative = new TrafficCampaignRequestDTO.Creative("cr-1", "val-1", true);
        var request = new TrafficCampaignRequestDTO(
                "camp-1",
                "CMIAP",
                TrafficCampaignRequestDTO.Platform.META_ADS,
                "Gerar leads",
                "Gestores B2B",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 15),
                budget,
                goals,
                List.of(creative),
                null
        );

        var response = new TrafficCampaignResponseDTO(
                "camp-1",
                "CMIAP",
                TrafficCampaignRequestDTO.Platform.META_ADS,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 15),
                TrafficCampaignResponseDTO.Status.SIMULATED_DRAFT,
                budget,
                goals,
                List.of(),
                "Estratégia",
                null
        );
        when(service.plan(any())).thenReturn(response);

        mockMvc.perform(post("/agents/traffic/campaign-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIMULATED_DRAFT"))
                .andExpect(jsonPath("$.campaignReference").value("camp-1"));

        mockMvc.perform(post("/agents/traffic/campaign-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

