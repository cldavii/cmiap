package ai.incorp.cmiap.agent.traffic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrafficCampaignResponseDTO(
        String campaignReference,
        String product,
        TrafficCampaignRequestDTO.Platform platform,
        LocalDate startDate,
        LocalDate endDate,
        Status status,
        TrafficCampaignRequestDTO.Budget budget,
        TrafficCampaignRequestDTO.Goals goals,
        List<String> issues,
        String strategy,
        Performance performance
) {
    public enum Status { SIMULATED_DRAFT, REVIEW_REQUIRED }
    public enum MetricStatus { MISSING, NO_GOAL, GOAL_MET, GOAL_NOT_MET }
    public enum GoalDirection { AT_MOST, AT_LEAST }

    public record Performance(
            String source,
            String reference,
            LocalDate startDate,
            LocalDate endDate,
            List<Metric> metrics,
            List<String> alerts
    ) {}

    public record Metric(
            String name,
            String unit,
            BigDecimal value,
            Goal goal,
            MetricStatus status
    ) {}

    public record Goal(BigDecimal value, GoalDirection direction) {}
}
