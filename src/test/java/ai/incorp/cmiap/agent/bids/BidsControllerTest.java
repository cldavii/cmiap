package ai.incorp.cmiap.agent.bids;

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

import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.RiskLevel.LOW;
import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.Status.VIABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BidsControllerTest {

    private final BidsAnalysisService service = mock(BidsAnalysisService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new BidsController(service))
                .setValidator(validator)
                .build();
    }

    @Test
    void postAnalyzeReturns200WithStructuredResponse() throws Exception {
        var response = new BidsAnalysisResponseDTO(
                "PE-10/2026",
                "Tribunal de Justiça",
                BidsAnalysisRequestDTO.Modality.PREGAO_ELETRONICO,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 1),
                VIABLE,
                LOW,
                List.of(),
                List.of(new BidsAnalysisResponseDTO.ChecklistItem(
                        "Contrato Social",
                        BidsAnalysisRequestDTO.DocumentType.HABILITACAO_JURIDICA,
                        BidsAnalysisResponseDTO.ChecklistStatus.COMPLIANT,
                        "OK"
                )),
                "Participação recomendada sem ressalvas."
        );
        when(service.analyze(any())).thenReturn(response);

        var request = new BidsAnalysisRequestDTO(
                "PE-10/2026",
                "Tribunal de Justiça",
                BidsAnalysisRequestDTO.Modality.PREGAO_ELETRONICO,
                "Serviços de IA para triagem processual",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 1),
                new BidsAnalysisRequestDTO.Budget("BRL", new BigDecimal("250000.00"), new BigDecimal("300000.00")),
                List.of("Atestado de capacidade técnica"),
                List.of(new BidsAnalysisRequestDTO.Document(
                        "Contrato Social",
                        BidsAnalysisRequestDTO.DocumentType.HABILITACAO_JURIDICA,
                        true,
                        null
                )),
                "Edital regular."
        );

        mockMvc.perform(post("/agents/bids/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidReference").value("PE-10/2026"))
                .andExpect(jsonPath("$.issuingBody").value("Tribunal de Justiça"))
                .andExpect(jsonPath("$.modality").value("PREGAO_ELETRONICO"))
                .andExpect(jsonPath("$.status").value("VIABLE"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.strategicRecommendations").value("Participação recomendada sem ressalvas."))
                .andExpect(jsonPath("$.checklist[0].item").value("Contrato Social"))
                .andExpect(jsonPath("$.checklist[0].status").value("COMPLIANT"));
    }

    @Test
    void postAnalyzeRejectsInvalidPayloadWith400() throws Exception {
        String invalidJson = """
                {
                  "bidReference": "",
                  "issuingBody": ""
                }
                """;

        mockMvc.perform(post("/agents/bids/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}

