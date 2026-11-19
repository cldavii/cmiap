package ai.incorp.cmiap.agent.bids;

import java.time.LocalDate;
import java.util.List;

public record BidsAnalysisResponseDTO(
        String bidReference,
        String issuingBody,
        BidsAnalysisRequestDTO.Modality modality,
        LocalDate submissionDeadline,
        LocalDate openingDate,
        Status status,
        RiskLevel riskLevel,
        List<String> issues,
        List<ChecklistItem> checklist,
        String strategicRecommendations
) {
    public enum Status {
        VIABLE,
        REVIEW_REQUIRED,
        HIGH_RISK
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum ChecklistStatus {
        COMPLIANT,
        PENDING,
        MISSING
    }

    public record ChecklistItem(
            String item,
            BidsAnalysisRequestDTO.DocumentType type,
            ChecklistStatus status,
            String observation
    ) {}
}

