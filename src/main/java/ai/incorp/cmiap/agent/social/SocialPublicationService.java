package ai.incorp.cmiap.agent.social;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
class SocialPublicationService {

    private final Clock clock;

    SocialPublicationService() {
        this(Clock.systemUTC());
    }

    SocialPublicationService(Clock clock) {
        this.clock = clock;
    }

    SocialPublicationResponseDTO check(SocialPublicationRequestDTO request) {
        var issues = new ArrayList<String>();
        checkCalendar(request, issues);
        if (!request.contentBrandVersion().strip().equals(request.currentBrandVersion().strip())) {
            issues.add("A versão das diretrizes de marca mudou desde a produção.");
        }
        if (request.platform() == SocialPublicationRequestDTO.Platform.LINKEDIN
                && request.format() != SocialPublicationRequestDTO.Format.POST) {
            issues.add("O plano para LinkedIn aceita apenas o formato POST.");
        }
        checkContent(request, issues);
        var status = SocialPublicationResponseDTO.Status.REVIEW_REQUIRED;
        if (issues.isEmpty()) {
            status = request.confirmed() ? SocialPublicationResponseDTO.Status.SIMULATED
                    : SocialPublicationResponseDTO.Status.READY_FOR_CONFIRMATION;
        }
        var simulation = status == SocialPublicationResponseDTO.Status.SIMULATED ? simulation(request) : null;
        return new SocialPublicationResponseDTO(status, request.platform(), request.format(),
                request.plannedAt(), List.copyOf(issues), request.technicalRules().reference(),
                simulation, pendingInteractions(request.interactions()));
    }

    private void checkCalendar(SocialPublicationRequestDTO request, List<String> issues) {
        OffsetDateTime plannedAt = request.plannedAt();
        if (!plannedAt.toInstant().isAfter(clock.instant())) {
            issues.add("O horário planejado deve estar no futuro.");
        }
        if (request.approvedSlots().stream().noneMatch(plannedAt::isEqual)) {
            issues.add("O horário não consta no calendário aprovado.");
        }
        Duration spacing = Duration.ofMinutes(request.minimumSpacingMinutes());
        if (request.occupiedSlots().stream().anyMatch(slot -> plannedAt.isEqual(slot)
                || Duration.between(plannedAt, slot).abs().compareTo(spacing) < 0)) {
            issues.add("Há conflito com outra publicação no horário ou no intervalo mínimo informado.");
        }
    }

    private SocialPublicationResponseDTO.Simulation simulation(SocialPublicationRequestDTO request) {
        String contentReference = request.contentReference().strip();
        String brandVersion = request.currentBrandVersion().strip();
        String key = "%s:%s:%s:%s:%s:%s".formatted(request.platform(), request.format(),
                request.plannedAt().toInstant(), contentReference.length(), contentReference, brandVersion);
        String reference = "social-sim-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        return new SocialPublicationResponseDTO.Simulation(reference, contentReference, brandVersion);
    }

    private List<SocialPublicationResponseDTO.PendingInteraction> pendingInteractions(
            List<SocialPublicationRequestDTO.Interaction> interactions) {
        if (interactions == null) {
            return List.of();
        }
        return interactions.stream()
                .filter(SocialPublicationRequestDTO.Interaction::pendingReply)
                .map(item -> new SocialPublicationResponseDTO.PendingInteraction(item.type(), item.reference(), item.url()))
                .toList();
    }

    private void checkContent(SocialPublicationRequestDTO request, List<String> issues) {
        var rules = request.technicalRules();
        var asset = request.asset();
        String caption = request.caption();
        if (caption.codePointCount(0, caption.length()) > rules.maxCaptionCharacters()) {
            issues.add("O texto excede o limite informado para a plataforma.");
        }
        if (caption.isBlank() && asset == null) {
            issues.add("Informe um texto finalizado ou um arquivo de mídia.");
        }
        if (asset == null) {
            if (rules.assetRequired() || request.format() != SocialPublicationRequestDTO.Format.POST
                    || request.platform() == SocialPublicationRequestDTO.Platform.INSTAGRAM) {
                issues.add("O formato exige um arquivo de mídia.");
            }
            return;
        }
        checkAsset(asset, rules, issues);
    }

    private void checkAsset(SocialPublicationRequestDTO.Asset asset,
                            SocialPublicationRequestDTO.TechnicalRules rules, List<String> issues) {
        if (rules.allowedMimeTypes().stream().noneMatch(asset.mimeType()::equalsIgnoreCase)) {
            issues.add("O tipo do arquivo não é permitido pelas regras informadas.");
        }
        if ((rules.requiredWidth() != null && rules.requiredWidth() != asset.width())
                || (rules.requiredHeight() != null && rules.requiredHeight() != asset.height())) {
            issues.add("As dimensões do arquivo divergem das regras informadas.");
        }
        if (rules.maxAssetBytes() != null && asset.sizeBytes() > rules.maxAssetBytes()) {
            issues.add("O arquivo excede o tamanho permitido pelas regras informadas.");
        }
    }
}
