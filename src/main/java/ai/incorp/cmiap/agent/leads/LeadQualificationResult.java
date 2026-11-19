package ai.incorp.cmiap.agent.leads;

import java.time.Instant;
import java.util.UUID;

public record LeadQualificationResult(
        UUID id,
        int score,
        LeadFit fit,
        String reasoning,
        String nextStep,
        Instant qualifiedAt
) {

    static LeadQualificationResult from(Lead lead) {
        return new LeadQualificationResult(
                lead.getId(),
                lead.getScore(),
                lead.getFit(),
                lead.getReasoning(),
                lead.getNextStep(),
                lead.getQualifiedAt()
        );
    }
}
