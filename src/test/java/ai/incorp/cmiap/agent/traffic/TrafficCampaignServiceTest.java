package ai.incorp.cmiap.agent.traffic;

import ai.incorp.cmiap.agent.reports.ReportRequestDTO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static ai.incorp.cmiap.agent.traffic.TrafficCampaignResponseDTO.MetricStatus.*;
import static ai.incorp.cmiap.agent.traffic.TrafficCampaignResponseDTO.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class TrafficCampaignServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final LocalDate END = START.plusDays(29);
    private final TrafficStrategyAgent agent = mock(TrafficStrategyAgent.class);
    private final TrafficCampaignService service = new TrafficCampaignService(agent, new ObjectMapper());

    @Test
    void suggestsAPlanWithTraceablePerformanceAndAlerts() {
        when(agent.suggest(argThat(briefing -> briefing.contains("exportacao-piloto")
                && briefing.contains("META_ADS")))).thenReturn("Teste a segmentação por setor.");
        var data = new TrafficCampaignRequestDTO.Observation("exportacao-piloto", START, END,
                2000L, 900L, 100L, 6L, money("60"), money("150"));

        var result = service.plan(request(money("50"), true, data));

        assertEquals(SIMULATED_DRAFT, result.status());
        assertEquals("Teste a segmentação por setor.", result.strategy());
        assertEquals("campanha-piloto", result.campaignReference());
        assertEquals("TRAFFIC_AGENT", result.performance().source());
        assertEquals("exportacao-piloto", result.performance().reference());
        assertEquals(GOAL_MET, metric(result, "CPA").status());
        assertEquals(GOAL_NOT_MET, metric(result, "ROAS").status());
        assertEquals(money("5.000000"), metric(result, "CTR").value());
        assertEquals(1, result.performance().alerts().size());
    }

    @Test
    void avoidsInventingMetricsForMissingValuesOrZeroDenominators() {
        when(agent.suggest(any())).thenReturn("Revise a coleta de conversões.");
        var data = new TrafficCampaignRequestDTO.Observation("exportacao-piloto", START, END,
                100L, null, null, 0L, BigDecimal.ZERO, null);

        var result = service.plan(request(money("50"), true, data));

        assertNull(metric(result, "CPA").value());
        assertEquals(MISSING, metric(result, "CPA").status());
        assertEquals(MISSING, metric(result, "ROAS").status());
        assertEquals(MISSING, metric(result, "Alcance").status());
        assertEquals(NO_GOAL, metric(result, "Conversões").status());
    }

    @Test
    void blocksAnOverBudgetDraftOrIncompatibleCreative() {
        var result = service.plan(request(money("100"), false, null));

        assertEquals(REVIEW_REQUIRED, result.status());
        assertEquals(2, result.issues().size());
        assertNull(result.strategy());
        assertNull(result.performance());
        verifyNoInteractions(agent);
    }

    @Test
    void passesTheDocumentedTrafficMetricsToTheReportContract() {
        when(agent.suggest(any())).thenReturn("Confira a consistência da amostra.");
        var data = new TrafficCampaignRequestDTO.Observation("campanha-piloto-exportacao-01", START, END,
                1L, null, 9_999_999_999_999_999L, null, money("60"), money("150"));
        var campaign = request(money("50"), true, data);
        var performance = service.plan(campaign).performance();
        var metrics = performance.metrics().stream().map(metric -> reported(metric, performance)).toList();
        var report = new ReportRequestDTO(campaign.product(), performance.startDate(), performance.endDate(),
                metrics, false);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(campaign).isEmpty());
            assertTrue(factory.getValidator().validate(report).isEmpty());
        }
    }

    @Test
    void rejectsObservationsOutsideTheCampaignPeriod() {
        var data = new TrafficCampaignRequestDTO.Observation("exportacao-piloto", START.minusDays(1), END,
                null, null, null, null, null, null);

        var error = assertThrows(ResponseStatusException.class, () -> service.plan(request(money("50"), true, data)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(agent);
    }

    private TrafficCampaignRequestDTO request(BigDecimal plannedBudget, boolean compatible,
                                              TrafficCampaignRequestDTO.Observation observation) {
        return new TrafficCampaignRequestDTO("campanha-piloto", "Produto piloto",
                TrafficCampaignRequestDTO.Platform.META_ADS, "Gerar procura", "Empresas de software",
                START, END, new TrafficCampaignRequestDTO.Budget("BRL", plannedBudget, money("60")),
                new TrafficCampaignRequestDTO.Goals(money("10"), money("3")),
                List.of(new TrafficCampaignRequestDTO.Creative("peca-1", "revisao-1", compatible)), observation);
    }

    private TrafficCampaignResponseDTO.Metric metric(TrafficCampaignResponseDTO result, String name) {
        return result.performance().metrics().stream().filter(item -> name.equals(item.name())).findFirst().orElseThrow();
    }

    private ReportRequestDTO.Metric reported(TrafficCampaignResponseDTO.Metric metric,
                                              TrafficCampaignResponseDTO.Performance performance) {
        var goal = metric.goal() == null ? null : new ReportRequestDTO.Goal(metric.goal().value(),
                ReportRequestDTO.GoalDirection.valueOf(metric.goal().direction().name()));
        var observations = metric.value() == null ? List.<ReportRequestDTO.Observation>of() : List.of(
                new ReportRequestDTO.Observation(ReportRequestDTO.Source.valueOf(performance.source()),
                        performance.reference(), metric.value()));
        return new ReportRequestDTO.Metric(ReportRequestDTO.Channel.PAID, metric.name(), metric.unit(),
                goal, observations, List.of());
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
