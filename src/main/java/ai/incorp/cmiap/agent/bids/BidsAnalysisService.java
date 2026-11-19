package ai.incorp.cmiap.agent.bids;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.ChecklistStatus.COMPLIANT;
import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.ChecklistStatus.MISSING;
import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.RiskLevel.*;
import static ai.incorp.cmiap.agent.bids.BidsAnalysisResponseDTO.Status.*;

@Service
public class BidsAnalysisService {

    private final BidsAgent agent;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public BidsAnalysisService(BidsAgent agent, ObjectMapper objectMapper) {
        this(agent, objectMapper, Clock.systemUTC());
    }

    public BidsAnalysisService(BidsAgent agent, ObjectMapper objectMapper, Clock clock) {
        this.agent = agent;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public BidsAnalysisResponseDTO analyze(BidsAnalysisRequestDTO request) {
        validateScope(request);
        var issues = evaluateIssues(request);
        var checklist = buildChecklist(request);
        var riskLevel = determineRiskLevel(issues, checklist);
        var status = determineStatus(riskLevel, issues);

        String recommendations = null;
        if (status != HIGH_RISK) {
            recommendations = generateRecommendations(request, issues, riskLevel);
        }

        return new BidsAnalysisResponseDTO(
                request.bidReference(),
                request.issuingBody(),
                request.modality(),
                request.submissionDeadline(),
                request.openingDate(),
                status,
                riskLevel,
                issues,
                checklist,
                recommendations
        );
    }

    private void validateScope(BidsAnalysisRequestDTO request) {
        if (request.submissionDeadline().isAfter(request.openingDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O prazo de envio de propostas não pode ser posterior à data de abertura.");
        }
        try {
            Currency.getInstance(request.budget().currency());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um código de moeda válido.", exception);
        }
    }

    private List<String> evaluateIssues(BidsAnalysisRequestDTO request) {
        var issues = new ArrayList<String>();
        LocalDate today = LocalDate.now(clock);

        if (request.submissionDeadline().isBefore(today)) {
            issues.add("O prazo de submissão do edital já expirou.");
        }

        var budget = request.budget();
        if (budget.maxAcceptableValue() != null
                && budget.estimatedValue().compareTo(budget.maxAcceptableValue()) > 0) {
            issues.add("O valor estimado ultrapassa o teto máximo aceitável para o edital.");
        }

        if (request.qualificationDocuments() != null) {
            for (var doc : request.qualificationDocuments()) {
                if (!Boolean.TRUE.equals(doc.provided())) {
                    issues.add("Documentação obrigatória pendente: " + doc.documentName());
                }
            }
        }

        return List.copyOf(issues);
    }

    private List<BidsAnalysisResponseDTO.ChecklistItem> buildChecklist(BidsAnalysisRequestDTO request) {
        if (request.qualificationDocuments() == null || request.qualificationDocuments().isEmpty()) {
            return List.of();
        }
        return request.qualificationDocuments().stream()
                .map(doc -> new BidsAnalysisResponseDTO.ChecklistItem(
                        doc.documentName(),
                        doc.type(),
                        Boolean.TRUE.equals(doc.provided()) ? COMPLIANT : MISSING,
                        Boolean.TRUE.equals(doc.provided())
                                ? "Documento informado e apto para verificação."
                                : "Documento ausente ou pendente de emissão/regularização."
                ))
                .toList();
    }

    private BidsAnalysisResponseDTO.RiskLevel determineRiskLevel(List<String> issues,
                                                                 List<BidsAnalysisResponseDTO.ChecklistItem> checklist) {
        long missingDocs = checklist.stream()
                .filter(item -> item.status() == MISSING)
                .count();

        boolean criticalIssue = issues.stream().anyMatch(issue ->
                issue.contains("expirou") || issue.contains("ultrapassa o teto máximo"));

        if (criticalIssue || missingDocs >= 3) {
            return HIGH;
        }
        if (!issues.isEmpty() || missingDocs > 0) {
            return MEDIUM;
        }
        return LOW;
    }

    private BidsAnalysisResponseDTO.Status determineStatus(BidsAnalysisResponseDTO.RiskLevel riskLevel, List<String> issues) {
        if (riskLevel == HIGH) {
            return HIGH_RISK;
        }
        if (!issues.isEmpty() || riskLevel == MEDIUM) {
            return REVIEW_REQUIRED;
        }
        return VIABLE;
    }

    private String generateRecommendations(BidsAnalysisRequestDTO request,
                                            List<String> issues,
                                            BidsAnalysisResponseDTO.RiskLevel riskLevel) {
        String briefing = objectMapper.writeValueAsString(new Briefing(request, issues, riskLevel));
        String result = agent.analyze(briefing);
        if (result == null || result.isBlank() || result.length() > 6000) {
            throw new IllegalStateException("O agente de Editais retornou uma análise inválida.");
        }
        return result.strip();
    }

    private record Briefing(
            BidsAnalysisRequestDTO edital,
            List<String> issuesIdentificadas,
            BidsAnalysisResponseDTO.RiskLevel nivelRisco
    ) {}
}

