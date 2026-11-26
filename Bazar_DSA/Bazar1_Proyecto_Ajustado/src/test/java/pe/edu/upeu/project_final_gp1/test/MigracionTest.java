package pe.edu.upeu.project_final_gp1.test;

import org.junit.jupiter.api.Test;
import pe.edu.upeu.project_final_gp1.implementacion.DeudorDaoImpl;
import pe.edu.upeu.project_final_gp1.implementacion.VentaDaoImpl;
import pe.edu.upeu.project_final_gp1.modelo.Deudor;
import pe.edu.upeu.project_final_gp1.modelo.MovimientoDeuda;

import java.io.InputStream;
import java.nio.file.*;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Abre una copia de la base del esquema antiguo (src/test/resources/legacy-bazar1.db) y comprueba la migración. */
class MigracionTest {
    @Test
    void migraLaBaseAntiguaSinPerderVentasDeudasNiCobros() throws Exception {
        Path dir = Files.createTempDirectory("bazar1-migra-");
        Path db = dir.resolve("bazar1.db");
        try (InputStream in = getClass().getResourceAsStream("/legacy-bazar1.db")) { Files.copy(in, db); }
        String previo = System.getProperty("app.db.path");
        System.setProperty("app.db.path", db.toString());
        try {
            // fuerza reinicialización con la nueva ruta
            var ventas = new VentaDaoImpl().listAll();
            assertEquals(4, ventas.size());
            assertTrue(Files.exists(dir.resolve("bazar1.db.respaldo-esquema-antiguo")), "debe dejar copia de seguridad");

            DeudorDaoImpl dao = new DeudorDaoImpl();
            List<Deudor> lista = dao.listAll();
            assertEquals(2, lista.size());
            Deudor pedro = lista.stream().filter(d -> d.getDeuda() > 0).findFirst().orElseThrow();
            assertEquals(50.0, pedro.getDeuda(), 0.001);   // 5 + 45 a crédito
            Deudor juan = lista.stream().filter(d -> d.getDeuda() == 0).findFirst().orElseThrow();
            assertEquals(10222, juan.getDni());            // debía 5 y pagó 5: queda como "Deuda Pagada"

            for (Deudor d : lista) {
                double ing = dao.listarIngresos(d.getIdDeudor()).stream().mapToDouble(MovimientoDeuda::monto).sum();
                double pag = dao.listarPagos(d.getIdDeudor()).stream().mapToDouble(MovimientoDeuda::monto).sum();
                assertEquals(d.getDeuda(), ing - pag, 0.001, d.getNombre());
            }
            try (Connection cn = pe.edu.upeu.project_final_gp1.utils.ConexionBD.getConnection(); Statement st = cn.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(rs.next(), "no debe haber referencias rotas");
            }
        } finally {
            if (previo == null) System.clearProperty("app.db.path"); else System.setProperty("app.db.path", previo);
        }
    }
}
