package ai.incorp.cmiap.agent.bids;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agents/bids")
@RequiredArgsConstructor
public class BidsController {

    private final BidsAnalysisService service;

    @PostMapping("/analyze")
    public BidsAnalysisResponseDTO analyze(@Valid @RequestBody BidsAnalysisRequestDTO request) {
        return service.analyze(request);
    }
}
