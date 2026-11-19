package ai.incorp.cmiap.agent.leads;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeadServiceTest {

    private final LeadRepository repository = mock(LeadRepository.class);
    private final LeadQualificationAgent agent = mock(LeadQualificationAgent.class);
    private final LeadService service = new LeadService(repository, agent, new ObjectMapper());
    private final UUID userId = UUID.randomUUID();
    private final UUID leadId = UUID.randomUUID();

    @Test
    void qualifiesOwnedLeadAgainstProvidedProfileAndPersistsResult() {
        Lead lead = new Lead(userId, new LeadRequest(
                "Marina Costa", "Acme", "Diretora", "marina@example.com", null, null,
                "Tecnologia", "20 a 200 funcionários", "São Paulo", null
        ));
        ReflectionTestUtils.setField(lead, "id", leadId);
        when(repository.findByIdAndUserId(leadId, userId)).thenReturn(Optional.of(lead));
        when(agent.qualify(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
                {"score":86,"fit":"QUALIFIED","reasoning":"Empresa e setor aderentes.","nextStep":"Agendar conversa de descoberta."}
                """);
        when(repository.save(lead)).thenReturn(lead);

        LeadQualificationResult result = service.qualify(
                userId,
                leadId,
                new LeadQualificationRequest("Empresas B2B de tecnologia com 20 a 200 funcionários.")
        );

        assertEquals(leadId, result.id());
        assertEquals(86, result.score());
        assertEquals(LeadFit.QUALIFIED, result.fit());
        assertEquals("Empresa e setor aderentes.", result.reasoning());
        assertEquals("Agendar conversa de descoberta.", result.nextStep());
        assertNotNull(result.qualifiedAt());
        verify(agent).qualify(argThat(prompt ->
                prompt.contains("Empresas B2B de tecnologia")
                        && prompt.contains("Acme")
                        && !prompt.contains("marina@example.com")
                        && !prompt.contains("Marina Costa")
        ));
        verify(repository).save(lead);
    }

    @Test
    void doesNotAllowQualifyingAnotherUsersLead() {
        when(repository.findByIdAndUserId(leadId, userId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.qualify(
                userId, leadId, new LeadQualificationRequest("Empresas B2B")
        ));
    }

    @Test
    void rejectsMalformedQualificationResponse() {
        Lead lead = new Lead(userId, new LeadRequest(
                "Marina Costa", null, null, null, null, null, null, null, null, null
        ));
        ReflectionTestUtils.setField(lead, "id", leadId);
        when(repository.findByIdAndUserId(leadId, userId)).thenReturn(Optional.of(lead));
        when(agent.qualify(org.mockito.ArgumentMatchers.anyString())).thenReturn("resposta sem JSON");

        assertThrows(IllegalStateException.class, () -> service.qualify(
                userId, leadId, new LeadQualificationRequest("Empresas B2B")
        ));
    }
}
