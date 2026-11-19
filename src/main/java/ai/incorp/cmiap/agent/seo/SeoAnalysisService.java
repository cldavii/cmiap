package ai.incorp.cmiap.agent.seo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeoAnalysisService {

    private final SeoPageFetcher pageFetcher;
    private final SeoAuditAgent agent;
    private final ObjectMapper objectMapper;

    public SeoAnalysisResult analyze(SeoAnalysisRequest request) {
        URI uri;
        try {
            uri = URI.create(request.url().trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A URL informada é inválida.", exception);
        }
        SeoPageSnapshot page = pageFetcher.fetch(uri);
        String prompt;
        try {
            prompt = """
                    URL publicada: %s
                    Palavra-chave-alvo: %s

                    Dados observados ao buscar a página:
                    <pagina>
                    %s
                    </pagina>

                    Texto complementar fornecido pelo usuário:
                    <texto_complementar>
                    %s
                    </texto_complementar>
                    """.formatted(
                    uri,
                    StringUtils.hasText(request.targetKeyword()) ? request.targetKeyword().trim() : "não informada",
                    objectMapper.writeValueAsString(page),
                    StringUtils.hasText(request.supplementalText())
                            ? request.supplementalText().trim()
                            : "não informado"
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("Não foi possível preparar a página para análise.", exception);
        }

        return parseAnalysis(agent.analyze(prompt), uri, page);
    }

    private SeoAnalysisResult parseAnalysis(String response, URI uri, SeoPageSnapshot page) {
        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException("O agente de SEO retornou uma resposta vazia.");
        }
        try {
            SeoAiAnalysis analysis = objectMapper.readValue(response, SeoAiAnalysis.class);
            if (analysis == null || analysis.score() == null
                    || analysis.score() < 0 || analysis.score() > 100
                    || !StringUtils.hasText(analysis.summary())
                    || !StringUtils.hasText(analysis.titleAdvice())
                    || !StringUtils.hasText(analysis.descriptionAdvice())
                    || !StringUtils.hasText(analysis.contentAdvice())
                    || !StringUtils.hasText(analysis.technicalAdvice())
                    || !StringUtils.hasText(analysis.keywordAdvice())
                    || analysis.actions() == null || analysis.actions().isEmpty()
                    || analysis.actions().size() > 8
                    || analysis.actions().stream().anyMatch(action -> !StringUtils.hasText(action))) {
                throw new IllegalStateException("O agente de SEO retornou dados inválidos.");
            }
            return new SeoAnalysisResult(
                    uri.toString(),
                    page.title(),
                    page.metaDescription(),
                    page.canonicalUrl(),
                    page.h1Count(),
                    page.headingCount(),
                    page.imagesWithoutAlt(),
                    page.internalLinks(),
                    page.externalLinks(),
                    analysis.score(),
                    analysis.summary().trim(),
                    analysis.titleAdvice().trim(),
                    analysis.descriptionAdvice().trim(),
                    analysis.contentAdvice().trim(),
                    analysis.technicalAdvice().trim(),
                    analysis.keywordAdvice().trim(),
                    analysis.actions().stream().map(String::trim).toList()
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("Não foi possível interpretar o resultado da análise de SEO.", exception);
        }
    }

    private record SeoAiAnalysis(
            Integer score,
            String summary,
            String titleAdvice,
            String descriptionAdvice,
            String contentAdvice,
            String technicalAdvice,
            String keywordAdvice,
            List<String> actions
    ) {
    }
}
