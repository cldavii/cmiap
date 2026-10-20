package ai.incorptech.cmiap.presentation.controller.dto.content;

import java.util.UUID;

public record ContentGenerationResponseDTO(UUID conversationId, String content) {}