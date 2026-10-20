package ai.incorptech.cmiap.presentation.controller.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ContentGenerationRequestDTO(
        UUID conversationId,

        @NotBlank(message = "O tema é obrigatório")
        @Size(max = 500, message = "O tema deve ter no máximo 500 caracteres")
        String topic,

        @NotBlank(message = "O formato é obrigatório")
        @Size(max = 100, message = "O formato deve ter no máximo 100 caracteres")
        String format,

        @Size(max = 100, message = "O produto deve ter no máximo 100 caracteres")
        String product,

        @Size(max = 100, message = "O canal deve ter no máximo 100 caracteres")
        String channel,

        @Size(max = 300, message = "O objetivo deve ter no máximo 300 caracteres")
        String objective,

        @Size(max = 300, message = "O público deve ter no máximo 300 caracteres")
        String audience,

        @Size(max = 4000, message = "As referências devem ter no máximo 4000 caracteres")
        String referenceFacts
) {
    public String toPrompt() {
        return """
                Tema: %s
                Formato: %s
                Produto: %s
                Canal: %s
                Objetivo: %s
                Público: %s
                Fatos e referências fornecidos: %s
                """.formatted(
                topic.trim(),
                format.trim(),
                valueOrNotInformed(product),
                valueOrNotInformed(channel),
                valueOrNotInformed(objective),
                valueOrNotInformed(audience),
                valueOrNotInformed(referenceFacts));
    }

    private static String valueOrNotInformed(String value) {
        return value == null || value.isBlank() ? "Não informado" : value.trim();
    }
}