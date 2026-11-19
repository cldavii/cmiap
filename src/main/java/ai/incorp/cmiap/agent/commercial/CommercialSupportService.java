package ai.incorp.cmiap.agent.commercial;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static ai.incorp.cmiap.agent.commercial.CommercialSupportResponseDTO.Status.READY;
import static ai.incorp.cmiap.agent.commercial.CommercialSupportResponseDTO.Status.REVIEW_REQUIRED;

@Service
public class CommercialSupportService {

    private final CommercialAgent agent;
    private final ObjectMapper objectMapper;

    public CommercialSupportService(CommercialAgent agent, ObjectMapper objectMapper) {
        this.agent = agent;
        this.objectMapper = objectMapper;
    }

    public CommercialSupportResponseDTO advise(CommercialSupportRequestDTO request) {
        validateScope(request);
        var issues = evaluateIssues(request);
        var status = issues.isEmpty() ? READY : REVIEW_REQUIRED;
        var objectionStrategies = mapObjections(request.objections());
        String strategy = generateStrategy(request, issues);

        return new CommercialSupportResponseDTO(
                request.opportunityReference(),
                request.clientName(),
                request.stage(),
                status,
                issues,
                objectionStrategies,
                strategy
        );
    }

    private void validateScope(CommercialSupportRequestDTO request) {
        try {
            Currency.getInstance(request.dealValue().currency());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um código de moeda válido.", exception);
        }
    }

    private List<String> evaluateIssues(CommercialSupportRequestDTO request) {
        var issues = new ArrayList<String>();

        if (request.dealValue().amount().compareTo(BigDecimal.ZERO) <= 0) {
            issues.add("O valor da oportunidade deve ser maior que zero.");
        }

        if (request.stage() == CommercialSupportRequestDTO.DealStage.CLOSING
                && request.objections() != null
                && !request.objections().isEmpty()) {
            issues.add("A oportunidade está em fase de fechamento mas possui objeções críticas em aberto.");
        }

        if (request.competitors() != null && request.competitors().size() >= 5) {
            issues.add("Cenário de alta concorrência: mais de 4 concorrentes mapeados na oportunidade.");
        }

        return List.copyOf(issues);
    }

    private List<CommercialSupportResponseDTO.ObjectionStrategy> mapObjections(
            List<CommercialSupportRequestDTO.Objection> objections) {
        if (objections == null || objections.isEmpty()) {
            return List.of();
        }
        return objections.stream()
                .map(obj -> new CommercialSupportResponseDTO.ObjectionStrategy(
                        obj.category(),
                        obj.description(),
                        resolveDefaultAngle(obj.category())
                ))
                .toList();
    }

    private String resolveDefaultAngle(String category) {
        String lower = category.toLowerCase();
        if (lower.contains("preço") || lower.contains("custo") || lower.contains("valor")) {
            return "Enfatizar retorno sobre investimento (ROI), TCO e custo de inação da empresa.";
        }
        if (lower.contains("prazo") || lower.contains("tempo") || lower.contains("demora")) {
            return "Apresentar cronograma por marcos graduais (quick wins) e suporte assistido à implantação.";
        }
        if (lower.contains("concorrente")) {
            return "Focar nos diferenciais comprovados de qualidade, suporte consultivo e segurança integrada.";
        }
        return "Conectar a proposta diretamente à dor prioritária e alinhar expectativas com o decisor chave.";
    }

    private String generateStrategy(CommercialSupportRequestDTO request, List<String> issues) {
        String briefing = objectMapper.writeValueAsString(new Briefing(request, issues));
        String recommendation = agent.advise(briefing);
        if (recommendation == null || recommendation.isBlank() || recommendation.length() > 6000) {
            throw new IllegalStateException("O agente de Apoio Comercial retornou uma recomendação inválida.");
        }
        return recommendation.strip();
    }

    private record Briefing(
            CommercialSupportRequestDTO oportunidade,
            List<String> alertasIdentificados
    ) {}
}

