package ai.incorp.cmiap.agent.social;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agents/social")
@RequiredArgsConstructor
public class SocialPublicationController {

    private final SocialPublicationService service;

    @PostMapping("/publication-plan")
    public SocialPublicationResponseDTO plan(@Valid @RequestBody SocialPublicationRequestDTO request) {
        return service.check(request);
    }
}
