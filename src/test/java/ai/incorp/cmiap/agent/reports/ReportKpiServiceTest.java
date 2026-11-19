package ai.incorp.cmiap.agent.reports;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static ai.incorp.cmiap.agent.reports.ReportRequestDTO.Channel.PAID;
import static ai.incorp.cmiap.agent.reports.ReportRequestDTO.GoalDirection.AT_MOST;
import static ai.incorp.cmiap.agent.reports.ReportRequestDTO.Source.META;
import static ai.incorp.cmiap.agent.reports.ReportRequestDTO.Source.TRAFFIC_AGENT;
import static ai.incorp.cmiap.agent.reports.ReportResponseDTO.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ReportKpiServiceTest {

    private final ReportNarrativeAgent agent = mock(ReportNarrativeAgent.class);
    private final ReportKpiService service = new ReportKpiService(agent, new ObjectMapper());

    @Test
    void checksTheTargetAndKeepsTheSource() {
        var metric = metric("CPA", observation("12.0"), observation("12.00"));
        var result = service.report(request(false, metric)).metrics().getFirst();

        assertEquals(GOAL_MET, result.status());
        assertEquals(new BigDecimal("12.0"), result.value());
        assertEquals("pacote-trafego-1", result.observations().getFirst().reference());
        verifyNoInteractions(agent);
    }

    @Test
    void keepsGapsAndConflictingValuesWithoutSelectingASource() {
        var missing = metric("CPA");
        var conflict = metric("CPC", observation("3"), observation("4"));
        var result = service.report(request(false, missing, conflict)).metrics();

        assertEquals(MISSING, result.getFirst().status());
        assertNull(result.getFirst().value());
        assertEquals(CONFLICT, result.getLast().status());
        assertNull(result.getLast().value());
        assertEquals(2, result.getLast().observations().size());
    }

    @Test
    void exposesAnUnmetGoalAndAValueWithoutGoal() {
        var noGoal = new ReportRequestDTO.Metric(PAID, "ROAS", "x", null, List.of(observation("3")), List.of());
        var result = service.report(request(false, metric("CPA", observation("15")), noGoal)).metrics();

        assertEquals(GOAL_NOT_MET, result.getFirst().status());
        assertEquals(NO_GOAL, result.getLast().status());
        assertEquals(new BigDecimal("3"), result.getLast().value());
    }

    @Test
    void rejectsAPaidMetricFromOutsideTraffic() {
        var invalid = metric("CPA", new ReportRequestDTO.Observation(META, "meta-1", BigDecimal.TEN));

        var error = assertThrows(ResponseStatusException.class, () -> service.report(request(false, invalid)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(agent);
    }

    @Test
    void sendsTheCalculatedResultAndSourceForOptionalNarrative() {
        when(agent.summarize(argThat(payload -> payload.contains("GOAL_MET")
                && payload.contains("pacote-trafego-1")))).thenReturn("CPA na meta [pacote-trafego-1].");

        var report = service.report(request(true, metric("CPA", observation("10"))));

        assertEquals("CPA na meta [pacote-trafego-1].", report.narrative());
        assertEquals(GOAL_MET, report.metrics().getFirst().status());
    }

    private ReportRequestDTO request(boolean narrative, ReportRequestDTO.Metric... metrics) {
        return new ReportRequestDTO("Produto piloto", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 18),
                List.of(metrics), narrative);
    }

    private ReportRequestDTO.Metric metric(String name, ReportRequestDTO.Observation... observations) {
        var goal = new ReportRequestDTO.Goal(BigDecimal.valueOf(12), AT_MOST);
        return new ReportRequestDTO.Metric(PAID, name, "BRL", goal, List.of(observations), List.of());
    }

    private ReportRequestDTO.Observation observation(String value) {
        return new ReportRequestDTO.Observation(TRAFFIC_AGENT, "pacote-trafego-1", new BigDecimal(value));
    }
}
