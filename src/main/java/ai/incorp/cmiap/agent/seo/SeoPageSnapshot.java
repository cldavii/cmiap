package ai.incorp.cmiap.agent.seo;

public record SeoPageSnapshot(
        String title,
        String metaDescription,
        String canonicalUrl,
        String pageText,
        int h1Count,
        int headingCount,
        int imagesWithoutAlt,
        int internalLinks,
        int externalLinks
) {
}
