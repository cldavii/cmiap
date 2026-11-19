package ai.incorp.cmiap.agent.commercial;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agents/commercial")
@RequiredArgsConstructor
public class CommercialSupportController {

    private final CommercialSupportService service;

    @PostMapping("/advise")
    public CommercialSupportResponseDTO advise(@Valid @RequestBody CommercialSupportRequestDTO request) {
        return service.advise(request);
    }
}

