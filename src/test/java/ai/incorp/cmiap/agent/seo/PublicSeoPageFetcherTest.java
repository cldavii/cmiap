package ai.incorp.cmiap.agent.seo;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class PublicSeoPageFetcherTest {

    @Test
    void rejectsNonHttpsLocalAndCredentialUrls() {
        assertThrows(ResponseStatusException.class,
                () -> PublicSeoPageFetcher.validatePublicHttpsUrl(URI.create("http://example.org")));
        assertThrows(ResponseStatusException.class,
                () -> PublicSeoPageFetcher.validatePublicHttpsUrl(URI.create("https://localhost")));
        assertThrows(ResponseStatusException.class,
                () -> PublicSeoPageFetcher.validatePublicHttpsUrl(URI.create("https://user:pass@example.org")));
        assertThrows(ResponseStatusException.class,
                () -> PublicSeoPageFetcher.validatePublicHttpsUrl(URI.create("https://127.0.0.1")));
    }

    @Test
    void dnsResolverRejectsLoopbackAndPrivateAddresses() throws Exception {
        PublicAddressDnsResolver resolver = new PublicAddressDnsResolver();

        assertThrows(java.net.UnknownHostException.class, () -> resolver.resolve("localhost"));
        assertFalse(PublicSeoPageFetcher.isPublicAddress(InetAddress.getByName("10.0.0.1")));
        assertFalse(PublicSeoPageFetcher.isPublicAddress(InetAddress.getByName("169.254.169.254")));
        assertFalse(PublicSeoPageFetcher.isPublicAddress(InetAddress.getByName("fc00::1")));
        assertTrue(PublicSeoPageFetcher.isPublicAddress(InetAddress.getByName("8.8.8.8")));
    }
}
