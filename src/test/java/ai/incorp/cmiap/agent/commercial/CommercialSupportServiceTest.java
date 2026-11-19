package ai.incorp.cmiap.agent.commercial;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static ai.incorp.cmiap.agent.commercial.CommercialSupportResponseDTO.Status.READY;
import static ai.incorp.cmiap.agent.commercial.CommercialSupportResponseDTO.Status.REVIEW_REQUIRED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CommercialSupportServiceTest {

    private final CommercialAgent agent = mock(CommercialAgent.class);
    private final CommercialSupportService service = new CommercialSupportService(agent, new ObjectMapper());

    @Test
    void advisesOpportunitySuccessfully() {
        when(agent.advise(anyString())).thenReturn("Enfatize a economia gerada em 12 meses e agende a demonstração técnica.");

        var request = new CommercialSupportRequestDTO(
                "OPP-001",
                "Acme Corp",
                "Logística",
                "Diretor de Operações e CFO",
                "Plataforma de Roteirização",
                CommercialSupportRequestDTO.DealStage.PROPOSAL,
                new CommercialSupportRequestDTO.DealValue("BRL", new BigDecimal("120000.00")),
                List.of("Alto custo de combustível", "Atrasos em entregas"),
                List.of("Concorrente A"),
                List.of(new CommercialSupportRequestDTO.Objection("Preço", "Acharam a mensalidade 15% acima da concorrência", "Enviamos tabela comparativa")),
                "Cliente busca decisão até o fim do trimestre."
        );

        var response = service.advise(request);

        assertEquals("OPP-001", response.opportunityReference());
        assertEquals("Acme Corp", response.clientName());
        assertEquals(READY, response.status());
        assertTrue(response.issues().isEmpty());
        assertEquals(1, response.objectionStrategies().size());
        assertTrue(response.objectionStrategies().getFirst().recommendedAngle().contains("ROI"));
        assertNotNull(response.commercialStrategy());
        verify(agent, times(1)).advise(anyString());
    }

    @Test
    void identifiesOpenObjectionsDuringClosingStageAsReviewRequired() {
        when(agent.advise(anyString())).thenReturn("Alinhe as pendências de contrato antes de solicitar assinatura.");

        var request = new CommercialSupportRequestDTO(
                "OPP-002",
                "Beta S.A.",
                "Varejo",
                "Head de Compras",
                "SaaS de Fidelização",
                CommercialSupportRequestDTO.DealStage.CLOSING, // Fechamento com objeção em aberto
                new CommercialSupportRequestDTO.DealValue("BRL", new BigDecimal("45000.00")),
                List.of("Churn alto"),
                List.of(),
                List.of(new CommercialSupportRequestDTO.Objection("Prazo de Implantação", "Exigem go-live em 10 dias", null)),
                null
        );

        var response = service.advise(request);

        assertEquals(REVIEW_REQUIRED, response.status());
        assertEquals(1, response.issues().size());
        assertTrue(response.issues().getFirst().contains("fase de fechamento"));
        assertNotNull(response.commercialStrategy());
    }

    @Test
    void rejectsInvalidCurrencyCode() {
        var request = new CommercialSupportRequestDTO(
                "OPP-003",
                "Empresa X",
                "Educação",
                "Reitor",
                "LMS",
                CommercialSupportRequestDTO.DealStage.PROSPECTING,
                new CommercialSupportRequestDTO.DealValue("INVALID", new BigDecimal("5000.00")),
                List.of("Evasão de alunos"),
                List.of(),
                List.of(),
                null
        );

        var ex = assertThrows(ResponseStatusException.class, () -> service.advise(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(agent);
    }

    @Test
    void throwsIllegalStateExceptionWhenAiReturnsInvalidResponse() {
        when(agent.advise(anyString())).thenReturn("   ");

        var request = new CommercialSupportRequestDTO(
                "OPP-004",
                "Empresa Y",
                "Finanças",
                "CTO",
                "API de Crédito",
                CommercialSupportRequestDTO.DealStage.DISCOVERY,
                new CommercialSupportRequestDTO.DealValue("USD", new BigDecimal("10000.00")),
                List.of("Latência na esteira de crédito"),
                List.of(),
                List.of(),
                null
        );

        var ex = assertThrows(IllegalStateException.class, () -> service.advise(request));
        assertTrue(ex.getMessage().contains("retornou uma recomendação inválida"));
    }
}
