package pe.edu.upeu.project_final_gp1.test;

import org.junit.jupiter.api.Test;
import pe.edu.upeu.project_final_gp1.implementacion.*;
import pe.edu.upeu.project_final_gp1.modelo.*;
import pe.edu.upeu.project_final_gp1.utils.ConexionBD;
import pe.edu.upeu.project_final_gp1.utils.Passwords;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegresionDaoTest extends BaseDaoTest {
    private final ProductoDaoImpl productos = new ProductoDaoImpl();
    private final VentaDaoImpl ventas = new VentaDaoImpl();
    private final DeudorDaoImpl deudores = new DeudorDaoImpl();

    private Venta venta(Producto p, int cant, double precio, int dni, double pagado) {
        Venta v = new Venta();
        v.setNombreCliente("Ana Perez"); v.setDniCliente(dni); v.setMontoPagado(pagado);
        v.agregarDetalle(new DetalleVenta(p.getId(), p.getNombre(), cant, precio));
        return v;
    }

    @Test
    void dniInvalidoNoSeGuardaEnNingunDao() {
        assertThrows(IllegalArgumentException.class, () -> new ClienteDaoImpl().guardarOActualizar(new Cliente("X", 0)));
        assertThrows(IllegalArgumentException.class, () -> new ClienteDaoImpl().guardarOActualizar(new Cliente("X", 123456789)));
        assertThrows(IllegalArgumentException.class, () -> deudores.registrar(new Deudor(0, "X", -5, 10)));
    }

    @Test
    void dniConCeroInicialSeEncuentraEscribiendoElCero() throws Exception {
        new ClienteDaoImpl().guardarOActualizar(new Cliente("Luis", 7654321));
        assertEquals(1, new ClienteDaoImpl().buscar("0765").size());
        assertEquals(1, new ClienteDaoImpl().buscar("07654321").size());
    }

    @Test
    void saldoSiempreEsIngresosMenosPagos() throws Exception {
        Producto p = new Producto("Arroz", 10.0, 50); productos.insert(p);
        ventas.registrarVenta(venta(p, 3, 10.0, 12345678, 5.0));          // crédito: 25
        Deudor d = deudores.buscar("12345678").get(0);
        deudores.aumentarDeuda(d.getIdDeudor(), 15.5, "Fideos");           // +15.5
        deudores.registrarPago(d.getIdDeudor(), 10.0);                     // -10
        double ing = deudores.listarIngresos(d.getIdDeudor()).stream().mapToDouble(MovimientoDeuda::monto).sum();
        double pag = deudores.listarPagos(d.getIdDeudor()).stream().mapToDouble(MovimientoDeuda::monto).sum();
        assertEquals(30.5, deudores.buscar("12345678").get(0).getDeuda(), 0.001);
        assertEquals(deudores.buscar("12345678").get(0).getDeuda(), ing - pag, 0.001);
        assertTrue(deudores.listarIngresos(d.getIdDeudor()).get(0).detalle().contains("Arroz x3"));
    }

    @Test
    void deudorPagadoSeConservaYPuedeVolverADeber() throws Exception {
        deudores.registrar(new Deudor(0, "Carlos", 11111111, 20.0));
        int id = deudores.buscar("11111111").get(0).getIdDeudor();
        deudores.registrarPago(id, 20.0);
        assertEquals(0.0, deudores.buscar("11111111").get(0).getDeuda(), 0.001);
        assertThrows(IllegalArgumentException.class, () -> deudores.registrarPago(id, 1.0));
        deudores.registrar(new Deudor(0, "Carlos", 11111111, 7.0));
        assertEquals(7.0, deudores.buscar("11111111").get(0).getDeuda(), 0.001);
        assertEquals(1, deudores.listarPagos(id).size());
    }

    @Test
    void registrarDeudorExistenteConDeudaPideUsarAumentar() throws Exception {
        deudores.registrar(new Deudor(0, "Carlos", 11111111, 20.0));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> deudores.registrar(new Deudor(0, "Carlos", 11111111, 5.0)));
        assertTrue(e.getMessage().contains("Aumentar deuda"));
    }

    @Test
    void telefonoSeConservaAlGuardarUnaVentaSinTelefono() throws Exception {
        deudores.registrar(new Deudor(0, "Carlos", 11111111, "900111222", 20.0));
        Producto p = new Producto("Sal", 2.0, 5); productos.insert(p);
        Venta v = venta(p, 1, 2.0, 11111111, 2.0); v.setNombreCliente("Carlos");
        ventas.registrarVenta(v);
        assertEquals("900111222", deudores.buscar("11111111").get(0).getTelefono());
    }

    @Test
    void precioCambiadoDespuesDeAgregarAlCarritoSeRechaza() throws Exception {
        Producto p = new Producto("Arroz", 10.0, 5); productos.insert(p);
        p.setPrecio(12.0); productos.update(p);
        assertThrows(IllegalArgumentException.class, () -> ventas.registrarVenta(venta(p, 1, 10.0, 12345678, 10.0)));
        assertEquals(5, productos.searchById(p.getId()).getStock());
        assertTrue(ventas.listAll().isEmpty());
    }

    @Test
    void noSePuedeEliminarProductoVendidoYElMensajeEsClaro() throws Exception {
        Producto p = new Producto("Arroz", 10.0, 5); productos.insert(p);
        ventas.registrarVenta(venta(p, 1, 10.0, 12345678, 10.0));
        SQLException e = assertThrows(SQLException.class, () -> productos.delete(p.getId()));
        assertTrue(e.getMessage().contains("ventas registradas"), e.getMessage());
        assertNotNull(productos.searchById(p.getId()));
    }

    @Test
    void productoInvalidoSeRechazaEnElDao() {
        assertThrows(IllegalArgumentException.class, () -> productos.insert(new Producto("  ", 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> productos.insert(new Producto("A", -1, 1)));
        assertThrows(IllegalArgumentException.class, () -> productos.insert(new Producto("A", Double.NaN, 1)));
    }

    @Test
    void busquedaNoInterpretaPorcentajeComoComodin() throws Exception {
        productos.insert(new Producto("Arroz", 1, 1)); productos.insert(new Producto("Azucar", 1, 1));
        assertTrue(productos.searchByNombre("%").isEmpty());
        assertTrue(productos.searchByNombre("_").isEmpty());
        assertEquals(1, productos.searchByNombre("arr").size());
    }

    @Test
    void usuarioInicialTieneHashYLoginFunciona() throws Exception {
        try (Connection cn = ConexionBD.getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT password FROM usuario WHERE user='admin'")) {
            assertTrue(rs.next());
            assertTrue(Passwords.esHash(rs.getString(1)));
        }
        UsuarioDaoImpl dao = new UsuarioDaoImpl();
        assertNotNull(dao.login("admin", "admin123"));
        assertNull(dao.login("admin", "mala"));
        assertNull(dao.login("nadie", "admin123"));
        assertNull(dao.login("admin' OR '1'='1", "x"));
    }

    @Test
    void contraseñaAntiguaEnTextoPlanoSeAceptaYSeConvierteEnHash() throws Exception {
        try (Connection cn = ConexionBD.getConnection(); Statement st = cn.createStatement()) {
            st.executeUpdate("INSERT INTO usuario(user,password,nombre,apellidos,tipoUser) VALUES('viejo','clave','V','V','Vendedor')");
        }
        assertNotNull(new UsuarioDaoImpl().login("viejo", "clave"));
        try (Connection cn = ConexionBD.getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT password FROM usuario WHERE user='viejo'")) {
            rs.next();
            assertTrue(Passwords.esHash(rs.getString(1)));
        }
        assertNotNull(new UsuarioDaoImpl().login("viejo", "clave"));
    }
}
