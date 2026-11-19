package ai.incorp.cmiap.agent.social;

import java.time.OffsetDateTime;
import java.util.List;

public record SocialPublicationResponseDTO(
        Status status,
        SocialPublicationRequestDTO.Platform platform,
        SocialPublicationRequestDTO.Format format,
        OffsetDateTime plannedAt,
        List<String> issues,
        String technicalRulesReference,
        Simulation simulation,
        List<PendingInteraction> pendingInteractions
) {
    public enum Status { REVIEW_REQUIRED, READY_FOR_CONFIRMATION, SIMULATED }

    public record Simulation(String reference, String contentReference, String brandVersion) {}

    public record PendingInteraction(SocialPublicationRequestDTO.InteractionType type, String reference, String url) {}
}
