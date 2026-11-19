package ai.incorp.cmiap.agent.traffic;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static ai.incorp.cmiap.agent.traffic.TrafficCampaignResponseDTO.Status.REVIEW_REQUIRED;
import static ai.incorp.cmiap.agent.traffic.TrafficCampaignResponseDTO.Status.SIMULATED_DRAFT;

@Service
@RequiredArgsConstructor
class TrafficCampaignService {

    private final TrafficStrategyAgent agent;
    private final ObjectMapper objectMapper;

    TrafficCampaignResponseDTO plan(TrafficCampaignRequestDTO request) {
        validateScope(request);
        var issues = issues(request);
        var status = issues.isEmpty() ? SIMULATED_DRAFT : REVIEW_REQUIRED;
        var performance = TrafficPerformance.evaluate(request);
        String strategy = issues.isEmpty() ? strategy(request, performance) : null;
        return new TrafficCampaignResponseDTO(request.campaignReference(), request.product(), request.platform(),
                request.startDate(), request.endDate(), status, request.budget(), request.goals(),
                issues, strategy, performance);
    }

    private void validateScope(TrafficCampaignRequestDTO request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O período da campanha é inválido.");
        }
        var data = request.observation();
        if (data != null && (data.endDate().isBefore(data.startDate())
                || data.startDate().isBefore(request.startDate()) || data.endDate().isAfter(request.endDate()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As observações devem estar no período da campanha.");
        }
        try {
            Currency.getInstance(request.budget().currency());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um código de moeda válido.", exception);
        }
    }

    private List<String> issues(TrafficCampaignRequestDTO request) {
        var issues = new ArrayList<String>();
        if (request.budget().planned().compareTo(request.budget().ceiling()) > 0) {
            issues.add("O orçamento planejado excede o teto informado.");
        }
        if (request.goals().maxCpa() == null && request.goals().minRoas() == null) {
            issues.add("Informe uma meta de CPA ou ROAS antes de estruturar a campanha.");
        }
        for (var creative : request.creatives()) {
            if (!creative.compatible()) {
                issues.add("Criativo incompatível com a plataforma: " + creative.reference());
            }
        }
        return List.copyOf(issues);
    }

    private String strategy(TrafficCampaignRequestDTO request, TrafficCampaignResponseDTO.Performance performance) {
        String briefing = objectMapper.writeValueAsString(new Briefing(request, performance));
        String recommendation = agent.suggest(briefing);
        if (recommendation == null || recommendation.isBlank() || recommendation.length() > 6000) {
            throw new IllegalStateException("O agente de Tráfego retornou uma estratégia inválida.");
        }
        return recommendation.strip();
    }

    private record Briefing(TrafficCampaignRequestDTO campaign, TrafficCampaignResponseDTO.Performance performance) {}
}
