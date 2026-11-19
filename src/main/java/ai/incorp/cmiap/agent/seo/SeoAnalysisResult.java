package ai.incorp.cmiap.agent.seo;

import java.util.List;

public record SeoAnalysisResult(
        String url,
        String title,
        String metaDescription,
        String canonicalUrl,
        int h1Count,
        int headingCount,
        int imagesWithoutAlt,
        int internalLinks,
        int externalLinks,
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
