package ai.incorp.cmiap.agent.leads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadQualificationRequest(
        @NotBlank @Size(max = 3000) String idealCustomerProfile
) {
}
