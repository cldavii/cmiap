package ai.incorp.cmiap.agent.traffic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static ai.incorp.cmiap.agent.traffic.TrafficCampaignResponseDTO.Goal;
import static ai.incorp.cmiap.agent.traffic.TrafficCampaignResponseDTO.GoalDirection.*;
import static ai.incorp.cmiap.agent.traffic.TrafficCampaignResponseDTO.Metric;
import static ai.incorp.cmiap.agent.traffic.TrafficCampaignResponseDTO.MetricStatus.*;

final class TrafficPerformance {

    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

    private TrafficPerformance() {}

    static TrafficCampaignResponseDTO.Performance evaluate(TrafficCampaignRequestDTO request) {
        var data = request.observation();
        if (data == null) {
            return null;
        }
        String currency = request.budget().currency();
        var targetCpa = request.goals().maxCpa();
        var targetRoas = request.goals().minRoas();
        var metrics = List.of(
                observed("Impressões", "impressões", decimal(data.impressions())),
                observed("Alcance", "pessoas", decimal(data.reach())),
                observed("Cliques", "cliques", decimal(data.clicks())),
                observed("Conversões", "conversões", decimal(data.conversions())),
                observed("Gasto", currency, data.spend()),
                observed("Receita atribuída", currency, data.attributedRevenue()),
                ratio("CPA", currency, data.spend(), decimal(data.conversions()),
                        targetCpa == null ? null : new Goal(targetCpa, AT_MOST)),
                ratio("ROAS", "x", data.attributedRevenue(), data.spend(),
                        targetRoas == null ? null : new Goal(targetRoas, AT_LEAST)),
                ratio("CPC", currency, data.spend(), decimal(data.clicks()), null),
                ratio("CTR", "%", data.clicks() == null ? null : decimal(data.clicks()).multiply(PERCENT),
                        decimal(data.impressions()), null)
        );
        return new TrafficCampaignResponseDTO.Performance("TRAFFIC_AGENT", data.reference(),
                data.startDate(), data.endDate(), metrics, alerts(request, data));
    }

    private static Metric observed(String name, String unit, BigDecimal value) {
        return new Metric(name, unit, value, null, value == null ? MISSING : NO_GOAL);
    }

    private static Metric ratio(String name, String unit, BigDecimal numerator, BigDecimal denominator, Goal goal) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return new Metric(name, unit, null, goal, MISSING);
        }
        BigDecimal value = numerator.divide(denominator, 6, RoundingMode.HALF_UP);
        if (goal == null) {
            return new Metric(name, unit, value, null, NO_GOAL);
        }
        int difference = value.compareTo(goal.value());
        boolean goalMet = goal.direction() == AT_MOST ? difference <= 0 : difference >= 0;
        return new Metric(name, unit, value, goal, goalMet ? GOAL_MET : GOAL_NOT_MET);
    }

    private static BigDecimal decimal(Long value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private static List<String> alerts(TrafficCampaignRequestDTO request, TrafficCampaignRequestDTO.Observation data) {
        if (data.spend() == null || data.spend().compareTo(request.budget().ceiling()) < 0) {
            return List.of();
        }
        return List.of("O gasto informado atingiu ou ultrapassou o teto do orçamento.");
    }
}
