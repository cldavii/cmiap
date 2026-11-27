package ai.incorp.cmiap.agent.traffic;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrafficPerformanceTest {

    @Test
    void returnsNullWhenObservationIsAbsent() {
        var request = baseRequest(null);
        assertNull(TrafficPerformance.evaluate(request));
    }

    @Test
    void evaluatesRatiosGoalsAndAlertsCorrectly() {
        var observation = new TrafficCampaignRequestDTO.Observation(
                "obs-1",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 20),
                1000L,
                800L,
                50L,
                10L,
                new BigDecimal("600.00"),
                new BigDecimal("1200.00")
        );

        var perf = TrafficPerformance.evaluate(baseRequest(observation));

        assertNotNull(perf);
        assertEquals("TRAFFIC_AGENT", perf.source());
        assertEquals(10, perf.metrics().size());

        var cpa = perf.metrics().stream().filter(m -> m.name().equals("CPA")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("60.000000"), cpa.value());
        assertEquals(TrafficCampaignResponseDTO.MetricStatus.GOAL_NOT_MET, cpa.status());

        var roas = perf.metrics().stream().filter(m -> m.name().equals("ROAS")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("2.000000"), roas.value());
        assertEquals(TrafficCampaignResponseDTO.MetricStatus.GOAL_NOT_MET, roas.status());

        assertTrue(perf.alerts().isEmpty());

        var overCeiling = new TrafficCampaignRequestDTO.Observation(
                "obs-2", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20),
                1000L, 800L, 50L, 10L, new BigDecimal("1500.00"), new BigDecimal("1200.00"));
        assertEquals(1, TrafficPerformance.evaluate(baseRequest(overCeiling)).alerts().size());
    }

    private static TrafficCampaignRequestDTO baseRequest(TrafficCampaignRequestDTO.Observation observation) {
        return new TrafficCampaignRequestDTO(
                "camp-1",
                "CMIAP",
                TrafficCampaignRequestDTO.Platform.META_ADS,
                "Gerar leads",
                "Gestores B2B",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                new TrafficCampaignRequestDTO.Budget("BRL", new BigDecimal("1000.00"), new BigDecimal("1500.00")),
                new TrafficCampaignRequestDTO.Goals(new BigDecimal("50.000000"), new BigDecimal("3.000000")),
                List.of(new TrafficCampaignRequestDTO.Creative("cr-1", "val-1", true)),
                observation
        );
    }
}
