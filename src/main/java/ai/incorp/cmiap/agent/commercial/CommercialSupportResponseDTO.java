package ai.incorp.cmiap.agent.commercial;

import java.util.List;

public record CommercialSupportResponseDTO(
        String opportunityReference,
        String clientName,
        CommercialSupportRequestDTO.DealStage stage,
        Status status,
        List<String> issues,
        List<ObjectionStrategy> objectionStrategies,
        String commercialStrategy
) {
    public enum Status {
        READY,
        REVIEW_REQUIRED
    }

    public record ObjectionStrategy(
            String category,
            String description,
            String recommendedAngle
    ) {}
}

