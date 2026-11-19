package ai.incorp.cmiap.agent.leads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 200) String company,
        @Size(max = 120) String jobTitle,
        @Email @Size(max = 254) String email,
        @Size(max = 50) String phone,
        @Size(max = 255) String website,
        @Size(max = 150) String industry,
        @Size(max = 100) String companySize,
        @Size(max = 200) String location,
        @Size(max = 2000) String notes
) {
}
