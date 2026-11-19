package ai.incorp.cmiap.agent.social;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static ai.incorp.cmiap.agent.social.SocialPublicationRequestDTO.Format.POST;
import static ai.incorp.cmiap.agent.social.SocialPublicationRequestDTO.Platform.LINKEDIN;
import static ai.incorp.cmiap.agent.social.SocialPublicationResponseDTO.Status.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialPublicationServiceTest {

    private static final OffsetDateTime PLANNED = OffsetDateTime.parse("2026-11-27T10:00:00-03:00");
    private final SocialPublicationService service = new SocialPublicationService(
            Clock.fixed(PLANNED.minusDays(1).toInstant(), ZoneOffset.UTC));

    @Test
    void asksForConfirmationWhenNoIssueIsFound() {
        var result = service.check(request(false, "v1", List.of(PLANNED), List.of(), null));

        assertEquals(READY_FOR_CONFIRMATION, result.status());
        assertNull(result.simulation());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void simulatesOnlyAfterConfirmationAndRecognizesEquivalentTimeZones() {
        var utcSlot = PLANNED.withOffsetSameInstant(ZoneOffset.UTC);
        var result = service.check(request(true, "v1", List.of(utcSlot), List.of(), null));
        var repeated = service.check(request(true, "v1", List.of(PLANNED), List.of(), null));

        assertEquals(SIMULATED, result.status());
        assertEquals("Limites do canal para o teste", result.technicalRulesReference());
        assertTrue(result.simulation().reference().startsWith("social-sim-"));
        assertEquals(repeated.simulation(), result.simulation());
        assertEquals("post-piloto", result.simulation().contentReference());
        assertEquals(List.of("comentario-1"), result.pendingInteractions().stream()
                .map(SocialPublicationResponseDTO.PendingInteraction::reference).toList());
    }

    @Test
    void blocksConflictingTimeAndOutdatedBrandVersionDespiteConfirmation() {
        var result = service.check(request(true, "v2", List.of(), List.of(PLANNED.plusMinutes(5)), null));

        assertEquals(REVIEW_REQUIRED, result.status());
        assertEquals(3, result.issues().size());
    }

    @Test
    void checksMediaAgainstTheSuppliedLimits() {
        var asset = new SocialPublicationRequestDTO.Asset("image/gif", 500, 500, 3_000);
        var result = service.check(request(true, "v1", List.of(PLANNED), List.of(), asset));

        assertEquals(REVIEW_REQUIRED, result.status());
        assertEquals(3, result.issues().size());
    }

    @Test
    void rejectsPastSchedule() {
        var reviewingLater = new SocialPublicationService(
                Clock.fixed(PLANNED.plusMinutes(1).toInstant(), ZoneOffset.UTC));

        assertEquals(REVIEW_REQUIRED, reviewingLater.check(
                request(true, "v1", List.of(PLANNED), List.of(), null)).status());
    }

    private SocialPublicationRequestDTO request(boolean confirmed, String brand, List<OffsetDateTime> approved,
                                               List<OffsetDateTime> occupied, SocialPublicationRequestDTO.Asset asset) {
        var rules = new SocialPublicationRequestDTO.TechnicalRules("Limites do canal para o teste", 300, false,
                List.of("image/png"), 1080, 1080, 1_000L);
        var interactions = List.of(
                new SocialPublicationRequestDTO.Interaction(SocialPublicationRequestDTO.InteractionType.COMMENT,
                        "comentario-1", "https://example.org/comment/1", true),
                new SocialPublicationRequestDTO.Interaction(SocialPublicationRequestDTO.InteractionType.DIRECT_MESSAGE,
                        "mensagem-2", null, false));
        return new SocialPublicationRequestDTO(LINKEDIN, POST, "post-piloto", "Conteúdo aprovado", asset, rules,
                "v1", brand, PLANNED, approved, occupied, 15, interactions, confirmed);
    }
}
