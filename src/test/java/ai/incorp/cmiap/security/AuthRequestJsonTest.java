package ai.incorp.cmiap.security;

import ai.incorp.cmiap.security.AuthRequest.AccessProfile;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class AuthRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveSerializarAuthRequestMarketingParaJson() throws Exception {
        AuthRequest request = new AuthRequest(
            "wendel@incorp.ai",
            "SenhaForte123",
            AccessProfile.MARKETING
        );

        String json = objectMapper.writeValueAsString(request);

        assertNotNull(json);
        assertTrue(json.contains("\"email\":\"wendel@incorp.ai\""));
        assertTrue(json.contains("\"password\":\"SenhaForte123\""));
        assertTrue(json.contains("\"department\":\"MARKETING\""));
    }

    @Test
    void deveDesserializarJsonParaAuthRequestCommercial() throws Exception {
        String jsonEntrada = """
            {
              "email": "comercial@incorp.ai",
              "password": "123456",
              "department": "COMMERCIAL"
            }
            """;

        AuthRequest resultado = objectMapper.readValue(jsonEntrada, AuthRequest.class);

        assertNotNull(resultado);
        assertEquals("comercial@incorp.ai", resultado.email());
        assertEquals("123456", resultado.password());
        assertEquals(AccessProfile.COMMERCIAL, resultado.department());
    }

    @Test
    void deveDesserializarJsonParaAuthRequestAdministrator() throws Exception {
        String jsonEntrada = """
            {
              "email": "admin@incorp.ai",
              "password": "123456",
              "department": "ADMINISTRATOR"
            }
            """;

        AuthRequest resultado = objectMapper.readValue(jsonEntrada, AuthRequest.class);

        assertNotNull(resultado);
        assertEquals(AccessProfile.ADMINISTRATOR, resultado.department());
    }

    @Test
    void deveFalharQuandoDepartmentForInvalido() {
        String jsonInvalido = """
            {
              "email": "teste@incorp.ai",
              "password": "123456",
              "department": "FINANCEIRO"
            }
            """;

        assertThrows(Exception.class, () -> {
            objectMapper.readValue(jsonInvalido, AuthRequest.class);
        });
    }
}