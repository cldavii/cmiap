package ai.incorp.cmiap.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDepartmentTest {

    @Test
    void deveConterDepartamentosMarketingECommercial() {
        UserDepartment[] departamentos = UserDepartment.values();

        assertEquals(2, departamentos.length);
        assertEquals(UserDepartment.MARKETING, UserDepartment.valueOf("MARKETING"));
        assertEquals(UserDepartment.COMMERCIAL, UserDepartment.valueOf("COMMERCIAL"));
    }

    @Test
    void deveLancarExcecaoParaDepartamentoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> {
            UserDepartment.valueOf("RH");
        });
    }
}
