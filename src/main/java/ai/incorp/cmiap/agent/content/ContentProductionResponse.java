package ai.incorp.cmiap.agent.content;

import java.util.UUID;

public record ContentProductionResponse(UUID conversationId, String draft, boolean humanReviewRequired) {
}
