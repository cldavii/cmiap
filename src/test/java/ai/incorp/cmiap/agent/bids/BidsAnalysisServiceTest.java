package ai.incorp.cmiap.agent.bids;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.ChecklistStatus.COMPLIANT;
import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.ChecklistStatus.MISSING;
import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.RiskLevel.HIGH;
import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.RiskLevel.LOW;
import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BidsAnalysisServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 11, 20);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
    private final BidsAgent agent = mock(BidsAgent.class);
    private final BidsAnalysisService service = new BidsAnalysisService(agent, new ObjectMapper(), clock);

    @Test
    void analyzesViableBidSuccessfully() {
        when(agent.analyze(anyString())).thenReturn("Edital sem vícios aparentes. Recomendada participação.");

        var request = new BidsAnalysisRequestDTO(
                "PE-01/2026",
                "Ministério da Saúde",
                BidsAnalysisRequestDTO.Modality.PREGAO_ELETRONICO,
                "Aquisição de soluções digitais em nuvem",
                BASE_DATE.plusDays(10),
                BASE_DATE.plusDays(10),
                new BidsAnalysisRequestDTO.Budget("BRL", new BigDecimal("500000.00"), new BigDecimal("600000.00")),
                List.of("Atestado de capacidade técnica em computação em nuvem"),
                List.of(
                        new BidsAnalysisRequestDTO.Document("Contrato Social", BidsAnalysisRequestDTO.DocumentType.HABILITACAO_JURIDICA, true, null),
                        new BidsAnalysisRequestDTO.Document("CND Federal", BidsAnalysisRequestDTO.DocumentType.REGULARIDADE_FISCAL_TRABALHISTA, true, null)
                ),
                "Edital regular para contratação de SaaS."
        );

        var response = service.analyze(request);

        assertEquals("PE-01/2026", response.bidReference());
        assertEquals(VIABLE, response.status());
        assertEquals(LOW, response.riskLevel());
        assertTrue(response.issues().isEmpty());
        assertEquals(2, response.checklist().size());
        assertEquals(COMPLIANT, response.checklist().getFirst().status());
        assertNotNull(response.strategicRecommendations());
        verify(agent, times(1)).analyze(anyString());
    }

    @Test
    void identifiesPendingDocumentsAndRequiresReview() {
        when(agent.analyze(anyString())).thenReturn("Atenção à regularização da CND.");

        var request = new BidsAnalysisRequestDTO(
                "PE-02/2026",
                "Prefeitura do Recife",
                BidsAnalysisRequestDTO.Modality.PREGAO_ELETRONICO,
                "Licenciamento de software de gestão",
                BASE_DATE.plusDays(15),
                BASE_DATE.plusDays(15),
                new BidsAnalysisRequestDTO.Budget("BRL", new BigDecimal("150000.00"), new BigDecimal("200000.00")),
                List.of("Comprovação de experiência"),
                List.of(
                        new BidsAnalysisRequestDTO.Document("Contrato Social", BidsAnalysisRequestDTO.DocumentType.HABILITACAO_JURIDICA, true, null),
                        new BidsAnalysisRequestDTO.Document("CND Municipal", BidsAnalysisRequestDTO.DocumentType.REGULARIDADE_FISCAL_TRABALHISTA, false, "Certidão vencida")
                ),
                null
        );

        var response = service.analyze(request);

        assertEquals(REVIEW_REQUIRED, response.status());
        assertEquals(1, response.issues().size());
        assertTrue(response.issues().getFirst().contains("CND Municipal"));
        assertEquals(MISSING, response.checklist().getLast().status());
        assertNotNull(response.strategicRecommendations());
    }

    @Test
    void identifiesExpiredDeadlineAsHighRiskWithoutCallingAi() {
        var request = new BidsAnalysisRequestDTO(
                "PE-03/2026",
                "Secretaria de Educação",
                BidsAnalysisRequestDTO.Modality.PREGAO_ELETRONICO,
                "Suporte a infraestrutura",
                BASE_DATE.minusDays(1), // Expirado
                BASE_DATE.plusDays(2),
                new BidsAnalysisRequestDTO.Budget("BRL", new BigDecimal("80000.00"), null),
                List.of(),
                List.of(),
                null
        );

        var response = service.analyze(request);

        assertEquals(HIGH_RISK, response.status());
        assertEquals(HIGH, response.riskLevel());
        assertTrue(response.issues().stream().anyMatch(issue -> issue.contains("já expirou")));
        assertNull(response.strategicRecommendations());
        verifyNoInteractions(agent);
    }

    @Test
    void rejectsSubmissionDeadlineAfterOpeningDate() {
        var request = new BidsAnalysisRequestDTO(
                "PE-04/2026",
                "Órgão Teste",
                BidsAnalysisRequestDTO.Modality.CONCORRENCIA,
                "Objeto de teste",
                BASE_DATE.plusDays(5),
                BASE_DATE.plusDays(2), // Abertura antes do envio!
                new BidsAnalysisRequestDTO.Budget("BRL", new BigDecimal("100000.00"), null),
                List.of(),
                List.of(),
                null
        );

        var ex = assertThrows(ResponseStatusException.class, () -> service.analyze(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("prazo de envio de propostas não pode ser posterior"));
        verifyNoInteractions(agent);
    }

    @Test
    void rejectsInvalidCurrencyCode() {
        var request = new BidsAnalysisRequestDTO(
                "PE-05/2026",
                "Órgão Teste",
                BidsAnalysisRequestDTO.Modality.DISPENSA,
                "Objeto de teste",
                BASE_DATE.plusDays(2),
                BASE_DATE.plusDays(2),
                new BidsAnalysisRequestDTO.Budget("XYZ", new BigDecimal("10000.00"), null),
                List.of(),
                List.of(),
                null
        );

        var ex = assertThrows(ResponseStatusException.class, () -> service.analyze(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(agent);
    }
}
