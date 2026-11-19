package ai.incorp.cmiap.agent.bids;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BidsAnalysisRequestDTO(
        @NotBlank @Size(max = 150) String bidReference,
        @NotBlank @Size(max = 150) String issuingBody,
        @NotNull Modality modality,
        @NotBlank @Size(max = 2000) String objectDescription,
        @NotNull LocalDate submissionDeadline,
        @NotNull LocalDate openingDate,
        @NotNull @Valid Budget budget,
        @Size(max = 20) List<@NotBlank @Size(max = 300) String> technicalRequirements,
        @Size(max = 30) List<@NotNull @Valid Document> qualificationDocuments,
        @Size(max = 4000) String noticeSummary
) {
    public enum Modality {
        PREGAO_ELETRONICO,
        CONCORRENCIA,
        DISPENSA,
        INEXIGIBILIDADE,
        DIALOGO_COMPETITIVO,
        CONCURSO,
        OUTROS
    }

    public enum DocumentType {
        HABILITACAO_JURIDICA,
        REGULARIDADE_FISCAL_TRABALHISTA,
        QUALIFICACAO_TECNICA,
        QUALIFICACAO_ECONOMICO_FINANCEIRA
    }

    public record Budget(
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull @DecimalMin("0.01") @Digits(integer = 14, fraction = 2) BigDecimal estimatedValue,
            @DecimalMin("0.01") @Digits(integer = 14, fraction = 2) BigDecimal maxAcceptableValue
    ) {}

    public record Document(
            @NotBlank @Size(max = 150) String documentName,
            @NotNull DocumentType type,
            @NotNull Boolean provided,
            @Size(max = 200) String remarks
    ) {}
}

