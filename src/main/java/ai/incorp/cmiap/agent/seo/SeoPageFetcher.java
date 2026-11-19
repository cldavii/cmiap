package ai.incorp.cmiap.agent.seo;

import java.net.URI;

public interface SeoPageFetcher {

    SeoPageSnapshot fetch(URI uri);
}
