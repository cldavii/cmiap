package ai.incorptech.cmiap.presentation.controller.dto.auth;

public record TokenResponseDTO(
        String token,
        String type
) {}
