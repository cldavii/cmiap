package ai.incorp.cmiap.agent.leads;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadQualificationAgent agent;
    private final ObjectMapper objectMapper;

    public List<LeadResponse> list(UUID userId) {
        return leadRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(LeadResponse::from)
                .toList();
    }

    public LeadResponse create(UUID userId, LeadRequest request) {
        return LeadResponse.from(leadRepository.save(new Lead(userId, request)));
    }

    public List<LeadResponse> importLeads(UUID userId, LeadImportRequest request) {
        List<Lead> leads = request.leads().stream()
                .map(lead -> new Lead(userId, lead))
                .toList();
        return leadRepository.saveAll(leads).stream()
                .map(LeadResponse::from)
                .toList();
    }

    public LeadQualificationResult qualify(UUID userId, UUID leadId, LeadQualificationRequest request) {
        Lead lead = leadRepository.findByIdAndUserId(leadId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead não encontrado."));
        String prompt;
        try {
            prompt = """
                    Perfil ideal de cliente informado pelo usuário:
                    <perfil>
                    %s
                    </perfil>

                    Dados do lead:
                    <lead>
                    %s
                    </lead>
                    """.formatted(
                    request.idealCustomerProfile(),
                    objectMapper.writeValueAsString(new LeadQualificationContext(
                            lead.getId(),
                            lead.getCompany(),
                            lead.getJobTitle(),
                            lead.getIndustry(),
                            lead.getCompanySize(),
                            lead.getLocation(),
                            lead.getNotes()
                    ))
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("Não foi possível preparar os dados do lead para qualificação.", exception);
        }

        LeadQualification qualification = parseQualification(agent.qualify(prompt));
        lead.setScore(qualification.score());
        lead.setFit(qualification.fit());
        lead.setReasoning(qualification.reasoning().trim());
        lead.setNextStep(qualification.nextStep().trim());
        lead.setQualifiedAt(Instant.now());
        return LeadQualificationResult.from(leadRepository.save(lead));
    }

    private LeadQualification parseQualification(String response) {
        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException("O agente de qualificação retornou uma resposta vazia.");
        }
        try {
            LeadQualification qualification = objectMapper.readValue(response, LeadQualification.class);
            if (qualification == null || qualification.score() == null
                    || qualification.score() < 0 || qualification.score() > 100
                    || qualification.fit() == null
                    || !StringUtils.hasText(qualification.reasoning())
                    || !StringUtils.hasText(qualification.nextStep())) {
                throw new IllegalStateException("O agente de qualificação retornou dados inválidos.");
            }
            return qualification;
        } catch (JacksonException exception) {
            throw new IllegalStateException("Não foi possível interpretar o resultado da qualificação.", exception);
        }
    }

    private record LeadQualificationContext(
            UUID id,
            String company,
            String jobTitle,
            String industry,
            String companySize,
            String location,
            String notes
    ) {
    }
}
