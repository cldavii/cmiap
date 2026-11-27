package ai.incorp.cmiap.agent.seo;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicAddressDnsResolverTest {

    private final PublicAddressDnsResolver resolver = new PublicAddressDnsResolver();

    @Test
    void rejectsLoopbackAndPrivateLiteralHostsAndResolvesPublicLiteral() throws UnknownHostException {
        assertThrows(UnknownHostException.class, () -> resolver.resolve("127.0.0.1"));
        assertThrows(UnknownHostException.class, () -> resolver.resolve("10.0.0.1"));
        assertThrows(UnknownHostException.class, () -> resolver.resolve("192.168.1.1"));

        InetAddress[] resolved = resolver.resolve("8.8.8.8");
        assertEquals(1, resolved.length);
        assertTrue(PublicSeoPageFetcher.isPublicAddress(resolved[0]));
    }
}

