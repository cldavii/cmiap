package ai.incorp.cmiap.agent.commercial;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CommercialSupportRequestDTO(
        @NotBlank @Size(max = 150) String opportunityReference,
        @NotBlank @Size(max = 150) String clientName,
        @NotBlank @Size(max = 100) String clientSegment,
        @NotBlank @Size(max = 500) String decisionMakers,
        @NotBlank @Size(max = 150) String productOffered,
        @NotNull DealStage stage,
        @NotNull @Valid DealValue dealValue,
        @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 300) String> painPoints,
        @Size(max = 10) List<@NotBlank @Size(max = 150) String> competitors,
        @Size(max = 10) List<@NotNull @Valid Objection> objections,
        @Size(max = 2000) String notes
) {
    public enum DealStage {
        PROSPECTING,
        DISCOVERY,
        PROPOSAL,
        NEGOTIATION,
        CLOSING
    }

    public record DealValue(
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull @DecimalMin("0.01") @Digits(integer = 14, fraction = 2) BigDecimal amount
    ) {}

    public record Objection(
            @NotBlank @Size(max = 150) String category,
            @NotBlank @Size(max = 500) String description,
            @Size(max = 300) String currentAttempt
    ) {}
}

