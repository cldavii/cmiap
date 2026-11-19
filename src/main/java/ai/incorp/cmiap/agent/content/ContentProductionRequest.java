package ai.incorp.cmiap.agent.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ContentProductionRequest(
        @NotBlank @Size(max = 500) String objective,
        @NotBlank @Size(max = 1000) String targetAudience,
        @NotBlank @Size(max = 100) String channel,
        @NotBlank @Size(max = 100) String format,
        @NotBlank @Size(max = 5000) String keyInformation,
        @Size(max = 200) String tone,
        @Size(max = 500) String callToAction,
        @Size(max = 2000) String additionalInstructions,
        @Size(max = 35) String language,
        UUID conversationId
) {
}
