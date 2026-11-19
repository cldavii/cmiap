package ai.incorp.cmiap.agent.commercial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static ai.incorp.cmiap.agent.commercial.CommercialSupportResponseDTO.Status.READY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommercialSupportControllerTest {

    private final CommercialSupportService service = mock(CommercialSupportService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new CommercialSupportController(service))
                .setValidator(validator)
                .build();
    }

    @Test
    void postAdviseReturns200WithStructuredResponse() throws Exception {
        var response = new CommercialSupportResponseDTO(
                "OPP-777",
                "LogTech S.A.",
                CommercialSupportRequestDTO.DealStage.PROPOSAL,
                READY,
                List.of(),
                List.of(new CommercialSupportResponseDTO.ObjectionStrategy(
                        "Preço",
                        "Setup elevado",
                        "Enfatizar ROI e redução de custos operacionais"
                )),
                "Apresente a proposta ancorada no valor gerado pelo ROI."
        );
        when(service.advise(any())).thenReturn(response);

        var request = new CommercialSupportRequestDTO(
                "OPP-777",
                "LogTech S.A.",
                "Transportes",
                "CFO e Gerente de Frotas",
                "SaaS de Otimização de Rotas",
                CommercialSupportRequestDTO.DealStage.PROPOSAL,
                new CommercialSupportRequestDTO.DealValue("BRL", new BigDecimal("75000.00")),
                List.of("Custo elevado com combustível"),
                List.of("Concorrente Beta"),
                List.of(new CommercialSupportRequestDTO.Objection("Preço", "Setup elevado", null)),
                "Decisão no próximo mês."
        );

        mockMvc.perform(post("/agents/commercial/advise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opportunityReference").value("OPP-777"))
                .andExpect(jsonPath("$.clientName").value("LogTech S.A."))
                .andExpect(jsonPath("$.stage").value("PROPOSAL"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.commercialStrategy").value("Apresente a proposta ancorada no valor gerado pelo ROI."))
                .andExpect(jsonPath("$.objectionStrategies[0].category").value("Preço"))
                .andExpect(jsonPath("$.objectionStrategies[0].recommendedAngle").isNotEmpty());
    }

    @Test
    void postAdviseRejectsInvalidPayloadWith400() throws Exception {
        String invalidJson = """
                {
                  "opportunityReference": "",
                  "clientName": ""
                }
                """;

        mockMvc.perform(post("/agents/commercial/advise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
