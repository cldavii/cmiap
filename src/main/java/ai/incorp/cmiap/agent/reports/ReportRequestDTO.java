package ai.incorp.cmiap.agent.reports;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportRequestDTO(
        @NotBlank @Size(max = 100) String product,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty @Size(max = 30) List<@NotNull @Valid Metric> metrics,
        boolean includeNarrative
) {
    public enum Channel { ORGANIC, PAID, LEADS, BIDS, SEO }
    public enum GoalDirection { AT_LEAST, AT_MOST }
    public enum Source { META, GOOGLE, LINKEDIN, TRAFFIC_AGENT, LEADS_AGENT, BIDS_AGENT, SEO_AGENT }

    public record Metric(
            @NotNull Channel channel,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 30) String unit,
            @Valid Goal goal,
            @NotNull @Size(max = 5) List<@NotNull @Valid Observation> observations,
            @Size(max = 5) List<@NotBlank @Size(max = 150) String> contextReferences
    ) {}

    public record Goal(
            @NotNull @Digits(integer = 18, fraction = 6) BigDecimal value,
            @NotNull GoalDirection direction
    ) {}

    public record Observation(
            @NotNull Source source,
            @NotBlank @Size(max = 150) String reference,
            @NotNull @Digits(integer = 18, fraction = 6) BigDecimal value
    ) {}
}
