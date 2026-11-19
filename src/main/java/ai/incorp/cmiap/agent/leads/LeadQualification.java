package ai.incorp.cmiap.agent.leads;

public record LeadQualification(
        Integer score,
        LeadFit fit,
        String reasoning,
        String nextStep
) {
}
