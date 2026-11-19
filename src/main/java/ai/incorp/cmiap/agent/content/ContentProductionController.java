package ai.incorp.cmiap.agent.content;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agents/content")
@RequiredArgsConstructor
public class ContentProductionController {

    private final ContentProductionService contentProductionService;

    @PostMapping("/produce")
    public ContentProductionResponse produce(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ContentProductionRequest request
    ) {
        return contentProductionService.produce(userId(jwt), request);
    }

    @GetMapping("/conversations")
    public List<ContentConversationSummary> listConversations(@AuthenticationPrincipal Jwt jwt) {
        return contentProductionService.listConversations(userId(jwt));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<ContentConversationMessageResponse> listMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId
    ) {
        return contentProductionService.listMessages(userId(jwt), conversationId);
    }

    private UUID userId(Jwt jwt) {
        String claim = jwt.getClaimAsString("user_id");
        if (claim == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "O token não identifica o usuário.");
        }
        try {
            return UUID.fromString(claim);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "O token contém um identificador inválido.", exception);
        }
    }
}
