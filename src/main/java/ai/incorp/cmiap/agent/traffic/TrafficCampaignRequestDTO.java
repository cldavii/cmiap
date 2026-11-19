package ai.incorp.cmiap.agent.traffic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrafficCampaignRequestDTO(
        @NotBlank @Size(max = 150) String campaignReference,
        @NotBlank @Size(max = 100) String product,
        @NotNull Platform platform,
        @NotBlank @Size(max = 1000) String objective,
        @NotBlank @Size(max = 1000) String audience,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Valid Budget budget,
        @NotNull @Valid Goals goals,
        @NotEmpty @Size(max = 10) List<@NotNull @Valid Creative> creatives,
        @Valid Observation observation
) {
    public enum Platform { META_ADS, GOOGLE_ADS, LINKEDIN_ADS }

    public record Budget(
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal planned,
            @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal ceiling
    ) {}

    public record Goals(
            @DecimalMin("0.000001") @Digits(integer = 12, fraction = 6) BigDecimal maxCpa,
            @DecimalMin("0.000001") @Digits(integer = 12, fraction = 6) BigDecimal minRoas
    ) {}

    public record Creative(
            @NotBlank @Size(max = 150) String reference,
            @NotBlank @Size(max = 150) String validationReference,
            @NotNull Boolean compatible
    ) {}

    public record Observation(
            @NotBlank @Size(max = 150) String reference,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @PositiveOrZero @Digits(integer = 16, fraction = 0) Long impressions,
            @PositiveOrZero @Digits(integer = 16, fraction = 0) Long reach,
            @PositiveOrZero @Digits(integer = 16, fraction = 0) Long clicks,
            @PositiveOrZero @Digits(integer = 16, fraction = 0) Long conversions,
            @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal spend,
            @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal attributedRevenue
    ) {}
}
