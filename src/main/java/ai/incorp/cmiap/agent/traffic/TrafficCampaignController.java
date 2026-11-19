package ai.incorp.cmiap.agent.traffic;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agents/traffic")
@RequiredArgsConstructor
public class TrafficCampaignController {

    private final TrafficCampaignService service;

    @PostMapping("/campaign-plan")
    public TrafficCampaignResponseDTO plan(@Valid @RequestBody TrafficCampaignRequestDTO request) {
        return service.plan(request);
    }
}
