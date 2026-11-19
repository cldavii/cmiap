package ai.incorp.cmiap.agent.social;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record SocialPublicationRequestDTO(
        @NotNull Platform platform,
        @NotNull Format format,
        @NotBlank @Size(max = 200) String contentReference,
        @NotNull @Size(max = 12000) String caption,
        @Valid Asset asset,
        @NotNull @Valid TechnicalRules technicalRules,
        @NotBlank @Size(max = 100) String contentBrandVersion,
        @NotBlank @Size(max = 100) String currentBrandVersion,
        @NotNull OffsetDateTime plannedAt,
        @NotNull @Size(max = 100) List<@NotNull OffsetDateTime> approvedSlots,
        @NotNull @Size(max = 100) List<@NotNull OffsetDateTime> occupiedSlots,
        @PositiveOrZero @Max(1440) int minimumSpacingMinutes,
        @Size(max = 100) List<@NotNull @Valid Interaction> interactions,
        boolean confirmed
) {
    public enum Platform { FACEBOOK, INSTAGRAM, LINKEDIN }
    public enum Format { POST, STORY, REEL }
    public enum InteractionType { COMMENT, DIRECT_MESSAGE }

    public record Interaction(
            @NotNull InteractionType type,
            @NotBlank @Size(max = 200) String reference,
            @Size(max = 1000) @Pattern(regexp = "https://\\S+") String url,
            boolean pendingReply
    ) {}

    public record Asset(
            @NotBlank @Size(max = 100) String mimeType,
            @Positive int width,
            @Positive int height,
            @Positive long sizeBytes
    ) {}

    public record TechnicalRules(
            @NotBlank @Size(max = 200) String reference,
            @Positive int maxCaptionCharacters,
            boolean assetRequired,
            @NotNull @Size(max = 10) List<@NotBlank @Size(max = 100) String> allowedMimeTypes,
            @Positive Integer requiredWidth,
            @Positive Integer requiredHeight,
            @Positive Long maxAssetBytes
    ) {}
}
