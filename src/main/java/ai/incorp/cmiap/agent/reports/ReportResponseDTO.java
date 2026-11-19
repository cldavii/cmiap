package ai.incorp.cmiap.agent.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportResponseDTO(
        String product,
        LocalDate startDate,
        LocalDate endDate,
        List<MetricResult> metrics,
        String narrative
) {
    public enum Status { MISSING, CONFLICT, NO_GOAL, GOAL_MET, GOAL_NOT_MET }

    public record MetricResult(
            ReportRequestDTO.Channel channel,
            String name,
            String unit,
            BigDecimal value,
            ReportRequestDTO.Goal goal,
            Status status,
            List<ReportRequestDTO.Observation> observations,
            List<String> contextReferences
    ) {}
}
