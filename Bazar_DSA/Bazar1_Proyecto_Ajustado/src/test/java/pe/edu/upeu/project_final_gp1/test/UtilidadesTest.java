package pe.edu.upeu.project_final_gp1.test;

import org.junit.jupiter.api.Test;
import pe.edu.upeu.project_final_gp1.utils.Dinero;
import pe.edu.upeu.project_final_gp1.utils.Dni;
import pe.edu.upeu.project_final_gp1.utils.Passwords;
import pe.edu.upeu.project_final_gp1.utils.Sql;

import static org.junit.jupiter.api.Assertions.*;

class UtilidadesTest {
    @Test
    void dniConCerosInicialesSeGuardaComoEnteroYSeMuestraCompleto() {
        assertEquals(7654321, Dni.parse("07654321"));
        assertEquals("07654321", Dni.format(7654321));
        assertEquals("12345678", Dni.format(12345678));
    }

    @Test
    void dniInvalidoSeRechaza() {
        for (String malo : new String[]{"", "  ", "1n", "10222", "123456789", "abcdefgh", "00000000", "-1234567", "1234 678"})
            assertThrows(IllegalArgumentException.class, () -> Dni.parse(malo), malo);
    }

    @Test
    void dineroEsEstricto() {
        assertEquals(12.5, Dinero.parse("12,5", "x"));
        assertEquals(0.5, Dinero.parse(" 0.50 ", "x"));
        for (String malo : new String[]{"12d", "1e3", "NaN", "0x1p3", "12.345", "-5", "1,000.00", "", null, ".5"})
            assertThrows(IllegalArgumentException.class, () -> Dinero.parse(malo, "x"), String.valueOf(malo));
    }

    @Test
    void contraseñaSeGuardaConHash() {
        String h = Passwords.hash("admin123");
        assertTrue(Passwords.esHash(h));
        assertFalse(h.contains("admin123"));
        assertTrue(Passwords.verificar("admin123", h));
        assertFalse(Passwords.verificar("otra", h));
        assertNotEquals(h, Passwords.hash("admin123"), "cada hash lleva su propia sal");
        assertFalse(Passwords.verificar("admin123", "admin123"), "un texto plano no es un hash válido");
    }

    @Test
    void comodinesDeLikeSeTratanComoTexto() {
        assertEquals("%100\\%%", Sql.contiene("100%"));
        assertEquals("%a\\_b%", Sql.contiene("a_b"));
    }
}
