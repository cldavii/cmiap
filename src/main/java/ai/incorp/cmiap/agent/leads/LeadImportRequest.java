package ai.incorp.cmiap.agent.leads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LeadImportRequest(
        @NotEmpty @Size(max = 100) List<@Valid LeadRequest> leads
) {
}
