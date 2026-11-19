package ai.incorp.cmiap.agent.reports;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static ai.incorp.cmiap.agent.reports.ReportResponseDTO.Status.*;

@Service
@RequiredArgsConstructor
class ReportKpiService {

    private final ReportNarrativeAgent agent;
    private final ObjectMapper objectMapper;

    ReportResponseDTO report(ReportRequestDTO request) {
        validateScope(request);
        var results = request.metrics().stream().map(this::evaluate).toList();
        String narrative = request.includeNarrative() ? narrative(results) : null;
        return new ReportResponseDTO(request.product(), request.startDate(), request.endDate(), results, narrative);
    }

    private void validateScope(ReportRequestDTO request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final não pode preceder a data inicial.");
        }
        var keys = new HashSet<String>();
        for (var metric : request.metrics()) {
            if (!keys.add(metric.channel() + ":" + metric.name().strip().toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Informe cada KPI uma única vez por canal e reúna todas as suas fontes no mesmo item.");
            }
            if (metric.observations().stream().anyMatch(item -> !isSourceAllowed(metric.channel(), item.source()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A fonte informada não pode fornecer valores para o canal deste KPI.");
            }
        }
    }

    private boolean isSourceAllowed(ReportRequestDTO.Channel channel, ReportRequestDTO.Source source) {
        return switch (source) {
            case META, GOOGLE, LINKEDIN -> channel == ReportRequestDTO.Channel.ORGANIC;
            case TRAFFIC_AGENT -> channel == ReportRequestDTO.Channel.PAID;
            case LEADS_AGENT -> channel == ReportRequestDTO.Channel.LEADS;
            case BIDS_AGENT -> channel == ReportRequestDTO.Channel.BIDS;
            case SEO_AGENT -> channel == ReportRequestDTO.Channel.SEO;
        };
    }

    private ReportResponseDTO.MetricResult evaluate(ReportRequestDTO.Metric metric) {
        var observations = metric.observations();
        BigDecimal value = observations.isEmpty() ? null : observations.getFirst().value();
        var status = status(value, metric);
        return new ReportResponseDTO.MetricResult(metric.channel(), metric.name(), metric.unit(),
                status == CONFLICT ? null : value, metric.goal(), status, List.copyOf(observations),
                metric.contextReferences() == null ? List.of() : List.copyOf(metric.contextReferences()));
    }

    private ReportResponseDTO.Status status(BigDecimal value, ReportRequestDTO.Metric metric) {
        if (value == null) {
            return MISSING;
        }
        if (metric.observations().stream().anyMatch(item -> item.value().compareTo(value) != 0)) {
            return CONFLICT;
        }
        return goalStatus(value, metric.goal());
    }

    private ReportResponseDTO.Status goalStatus(BigDecimal value, ReportRequestDTO.Goal goal) {
        if (goal == null) {
            return NO_GOAL;
        }
        int comparison = value.compareTo(goal.value());
        boolean met = goal.direction() == ReportRequestDTO.GoalDirection.AT_LEAST
                ? comparison >= 0 : comparison <= 0;
        return met ? GOAL_MET : GOAL_NOT_MET;
    }

    private String narrative(List<ReportResponseDTO.MetricResult> results) {
        String summary = agent.summarize(objectMapper.writeValueAsString(results));
        if (summary == null || summary.isBlank() || summary.length() > 4000) {
            throw new IllegalStateException("O agente de Relatórios e KPIs retornou uma leitura inválida.");
        }
        return summary.strip();
    }
}
