package ai.incorp.cmiap.agent.seo;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Locale;

@Component
public class PublicSeoPageFetcher implements SeoPageFetcher {

    private static final int CONNECT_TIMEOUT_MILLIS = 4000;
    private static final int READ_TIMEOUT_MILLIS = 5000;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_PAGE_TEXT_CHARS = 12000;

    @Override
    public SeoPageSnapshot fetch(URI uri) {
        validatePublicHttpsUrl(uri);
        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet request = new HttpGet(uri);
            request.setHeader("User-Agent", "CMIAP-SEO-Audit/1.0");
            request.setHeader("Accept", "text/html, application/xhtml+xml");
            request.setHeader("Accept-Encoding", "identity");
            request.setConfig(RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MILLIS))
                    .build());

            return client.execute(request, response -> {
                int status = response.getCode();
                if (status >= 300 && status < 400) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                            "A página redirecionou a solicitação. Informe a URL final usando HTTPS.");
                }
                if (status < 200 || status >= 300) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "O site não permitiu a leitura da página (HTTP " + status + ").");
                }

                var entity = response.getEntity();
                String contentType = entity == null ? null : entity.getContentType();
                if (contentType == null
                        || !(contentType.toLowerCase(Locale.ROOT).contains("text/html")
                        || contentType.toLowerCase(Locale.ROOT).contains("application/xhtml+xml"))) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                            "A URL não retornou uma página HTML.");
                }
                long contentLength = entity.getContentLength();
                if (contentLength > MAX_RESPONSE_BYTES) {
                    throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                            "A página excede o limite de 2 MB para análise.");
                }

                byte[] body;
                try (InputStream input = entity.getContent();
                     var output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int total = 0;
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        total += read;
                        if (total > MAX_RESPONSE_BYTES) {
                            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                                    "A página excede o limite de 2 MB para análise.");
                        }
                        output.write(buffer, 0, read);
                    }
                    body = output.toByteArray();
                }
                return snapshot(uri, body, contentType);
            });
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível carregar a página. Verifique a URL e tente novamente.", exception);
        }
    }

    static void validatePublicHttpsUrl(URI uri) {
        if (uri == null
                || !"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getPort() != -1 && uri.getPort() != 443
                || uri.getHost().contains(":")
                || uri.getHost().matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            throw invalidUrl();
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("localhost") || host.endsWith(".localhost")
                || host.endsWith(".local") || host.endsWith(".internal")
                || host.endsWith(".test") || host.endsWith(".example")) {
            throw invalidUrl();
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (!isPublicAddress(address)) {
                    throw invalidUrl();
                }
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "O domínio da URL não pôde ser resolvido.", exception);
        }
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first != 0 && first != 10 && first != 127
                    && !(first == 100 && second >= 64 && second <= 127)
                    && !(first == 169 && second == 254)
                    && !(first == 172 && second >= 16 && second <= 31)
                    && !(first == 192 && (second == 0 || second == 168))
                    && !(first == 198 && (second == 18 || second == 19))
                    && !(first == 198 && second == 51 && Byte.toUnsignedInt(bytes[2]) == 100)
                    && !(first == 203 && second == 0 && Byte.toUnsignedInt(bytes[2]) == 113)
                    && !(first == 192 && second == 88 && Byte.toUnsignedInt(bytes[2]) == 99)
                    && first < 224;
        }
        if (address instanceof Inet6Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            boolean documentationRange = bytes[0] == 0x20 && bytes[1] == 0x01
                    && bytes[2] == 0x0D && bytes[3] == (byte) 0xB8;
            return (first & 0xE0) == 0x20 && !documentationRange;
        }
        return false;
    }

    private SeoPageSnapshot snapshot(URI requestedUri, byte[] body, String contentType) {
        String charset = "UTF-8";
        int charsetIndex = contentType.toLowerCase(Locale.ROOT).indexOf("charset=");
        if (charsetIndex >= 0) {
            charset = contentType.substring(charsetIndex + 8).split("[;\\s]", 2)[0].replace("\"", "");
        }
        Charset pageCharset;
        try {
            pageCharset = Charset.forName(charset);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "A página retornou uma codificação de texto inválida.", exception);
        }
        Document document = Jsoup.parse(new String(body, pageCharset), requestedUri.toString());
        document.select("script, style, noscript, template, svg, iframe, canvas").remove();
        String title = document.title();
        Element descriptionElement = document.selectFirst("meta[name=description]");
        String description = descriptionElement == null ? "" : descriptionElement.attr("content").trim();
        Element canonical = document.selectFirst("link[rel=canonical]");
        String canonicalUrl = canonical == null ? "" : canonical.absUrl("href");
        String pageText = document.body().text();
        if (pageText.length() > MAX_PAGE_TEXT_CHARS) {
            pageText = pageText.substring(0, MAX_PAGE_TEXT_CHARS);
        }

        String baseHost = requestedUri.getHost();
        int internalLinks = 0;
        int externalLinks = 0;
        for (Element link : document.select("a[href]")) {
            String absoluteUrl = link.absUrl("href");
            String host;
            try {
                host = absoluteUrl.isBlank() ? "" : URI.create(absoluteUrl).getHost();
            } catch (IllegalArgumentException exception) {
                host = "";
            }
            if (host == null || host.equalsIgnoreCase(baseHost)) {
                internalLinks++;
            } else {
                externalLinks++;
            }
        }
        return new SeoPageSnapshot(
                title,
                description,
                canonicalUrl,
                pageText,
                document.select("h1").size(),
                document.select("h1, h2, h3, h4, h5, h6").size(),
                document.select("img:not([alt]), img[alt='']").size(),
                internalLinks,
                externalLinks
        );
    }

    private static ResponseStatusException invalidUrl() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Informe uma URL HTTPS válida de um domínio público, sem usuário, senha ou porta personalizada.");
    }

    private static CloseableHttpClient createHttpClient() {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(new PublicAddressDnsResolver())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MILLIS))
                        .setSocketTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MILLIS))
                        .build())
                .build();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableRedirectHandling()
                .disableContentCompression()
                .build();
    }
}
