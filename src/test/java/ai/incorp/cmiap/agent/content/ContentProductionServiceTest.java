package ai.incorp.cmiap.agent.content;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentProductionServiceTest {

    private final ContentProductionAgent agent = mock(ContentProductionAgent.class);
    private final ContentConversationService conversationService = mock(ContentConversationService.class);
    private final ContentProductionService service = new ContentProductionService(agent, conversationService);
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    @Test
    void createsDraftFromProvidedBriefAndRequiresHumanReview() {
        ContentProductionRequest request = new ContentProductionRequest(
                "Apresentar o serviço", "Pequenas empresas", "LinkedIn", "Publicação curta",
                "Serviço de consultoria", "Profissional", "Entre em contato", null, "pt-BR", null
        );
        when(conversationService.getOrCreate(userId, null, request.objective())).thenReturn(conversationId);
        when(agent.generate(anyString(), anyString())).thenReturn("Rascunho de conteúdo");

        ContentProductionResponse response = service.produce(userId, request);

        assertEquals(conversationId, response.conversationId());
        assertEquals("Rascunho de conteúdo", response.draft());
        assertTrue(response.humanReviewRequired());
        verify(agent).generate(org.mockito.ArgumentMatchers.eq(userId + ":" + conversationId),
                org.mockito.ArgumentMatchers.argThat(prompt ->
                prompt.contains("Objetivo: Apresentar o serviço")
                        && prompt.contains("Informações fornecidas: Serviço de consultoria")
                        && prompt.contains("Instruções adicionais: não informado")
                        && prompt.contains("Idioma: pt-BR")));
        verify(conversationService).recordExchange(userId, conversationId, request, "Rascunho de conteúdo");
    }

    @Test
    void rejectsEmptyModelResponse() {
        ContentProductionRequest request = new ContentProductionRequest(
                "Objetivo", "Público", "Canal", "Formato", "Informações", null, null, null, null, null
        );
        when(conversationService.getOrCreate(userId, null, request.objective())).thenReturn(conversationId);
        when(agent.generate(anyString(), anyString())).thenReturn("  ");

        assertThrows(IllegalStateException.class, () -> service.produce(userId, request));
    }
}
