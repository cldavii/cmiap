package ai.incorp.cmiap.agent.leads;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String name,
        String company,
        String jobTitle,
        String email,
        String phone,
        String website,
        String industry,
        String companySize,
        String location,
        String notes,
        Integer score,
        LeadFit fit,
        String reasoning,
        String nextStep,
        Instant qualifiedAt,
        Instant createdAt,
        Instant updatedAt
) {

    static LeadResponse from(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.getCompany(),
                lead.getJobTitle(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getWebsite(),
                lead.getIndustry(),
                lead.getCompanySize(),
                lead.getLocation(),
                lead.getNotes(),
                lead.getScore(),
                lead.getFit(),
                lead.getReasoning(),
                lead.getNextStep(),
                lead.getQualifiedAt(),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }
}
