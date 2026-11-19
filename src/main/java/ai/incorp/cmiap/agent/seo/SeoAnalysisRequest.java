package ai.incorp.cmiap.agent.seo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SeoAnalysisRequest(
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 200) String targetKeyword,
        @Size(max = 10000) String supplementalText
) {
}
