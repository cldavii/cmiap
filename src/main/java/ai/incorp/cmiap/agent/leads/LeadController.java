package ai.incorp.cmiap.agent.leads;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agents/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public List<LeadResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return leadService.list(userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LeadRequest request
    ) {
        return leadService.create(userId(jwt), request);
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public List<LeadResponse> importLeads(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LeadImportRequest request
    ) {
        return leadService.importLeads(userId(jwt), request);
    }

    @PostMapping("/{leadId}/qualify")
    public LeadQualificationResult qualify(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID leadId,
            @Valid @RequestBody LeadQualificationRequest request
    ) {
        return leadService.qualify(userId(jwt), leadId, request);
    }

    private UUID userId(Jwt jwt) {
        String claim = jwt.getClaimAsString("user_id");
        if (claim == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "O token não identifica o usuário.");
        }
        try {
            return UUID.fromString(claim);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "O token contém um identificador inválido.",
                    exception
            );
        }
    }
}
