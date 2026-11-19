package ai.incorp.cmiap.agent.seo;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class SeoAnalysisServiceTest {

    private final SeoPageFetcher pageFetcher = mock(SeoPageFetcher.class);
    private final SeoAuditAgent agent = mock(SeoAuditAgent.class);
    private final SeoAnalysisService service = new SeoAnalysisService(pageFetcher, agent, new ObjectMapper());

    @Test
    void combinesPageSignalsWithValidatedAiRecommendations() {
        when(pageFetcher.fetch(any(URI.class))).thenReturn(new SeoPageSnapshot(
                "Página publicada", "Descrição atual", "https://example.org/", "conteúdo", 1, 4, 2, 3, 1
        ));
        when(agent.analyze(any())).thenReturn("""
                {"score":82,"summary":"Resumo","titleAdvice":"Título","descriptionAdvice":"Descrição","contentAdvice":"Conteúdo","technicalAdvice":"Técnica","keywordAdvice":"Palavra-chave","actions":["Ação 1"]}
                """);

        SeoAnalysisResult result = service.analyze(new SeoAnalysisRequest(
                "https://example.org/", "marketing", "Público-alvo"
        ));

        assertEquals(82, result.score());
        assertEquals("Página publicada", result.title());
        assertEquals(2, result.imagesWithoutAlt());
        assertEquals("Ação 1", result.actions().getFirst());
        verify(agent).analyze(argThat(prompt ->
                prompt.contains("marketing")
                        && prompt.contains("Público-alvo")
                        && prompt.contains("Página publicada")
        ));
    }

    @Test
    void rejectsInvalidAiScoreAndMalformedUrl() {
        when(pageFetcher.fetch(any(URI.class))).thenReturn(new SeoPageSnapshot(
                "", "", "", "", 0, 0, 0, 0, 0
        ));
        when(agent.analyze(any())).thenReturn("""
                {"score":101,"summary":"Resumo","titleAdvice":"Título","descriptionAdvice":"Descrição","contentAdvice":"Conteúdo","technicalAdvice":"Técnica","keywordAdvice":"Palavra-chave","actions":["Ação"]}
                """);

        assertThrows(IllegalStateException.class,
                () -> service.analyze(new SeoAnalysisRequest("https://example.org/", null, null)));
        assertThrows(ResponseStatusException.class,
                () -> service.analyze(new SeoAnalysisRequest("https://bad url", null, null)));
    }
}
