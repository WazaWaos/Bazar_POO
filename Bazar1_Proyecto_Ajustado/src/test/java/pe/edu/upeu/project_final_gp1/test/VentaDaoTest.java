package pe.edu.upeu.project_final_gp1.test;

import org.junit.jupiter.api.Test;
import pe.edu.upeu.project_final_gp1.implementacion.DeudorDaoImpl;
import pe.edu.upeu.project_final_gp1.implementacion.ProductoDaoImpl;
import pe.edu.upeu.project_final_gp1.implementacion.VentaDaoImpl;
import pe.edu.upeu.project_final_gp1.modelo.DetalleVenta;
import pe.edu.upeu.project_final_gp1.modelo.Producto;
import pe.edu.upeu.project_final_gp1.modelo.Venta;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class VentaDaoTest extends BaseDaoTest {
    private final ProductoDaoImpl productos = new ProductoDaoImpl();
    private final VentaDaoImpl ventas = new VentaDaoImpl();

    @Test
    void registrarVentaActualizaStockYDeuda() throws Exception {

        Producto producto = new Producto("Arroz", 10.00, 5);
        assertTrue(productos.insert(producto));

        Venta venta = new Venta();
        venta.setFecha(LocalDate.now().toString());
        venta.setNombreCliente("Ana Perez");
        venta.setDniCliente(12345678);
        venta.setMontoPagado(5.00);

        venta.agregarDetalle(
                new DetalleVenta(producto.getId(), "Arroz", 2, 10.00));

        Venta guardada = ventas.registrarVenta(venta);

        assertTrue(guardada.getIdVenta() > 0);
        assertEquals(20.00, guardada.getTotal(), 0.001);
        assertEquals(5.00, guardada.getMontoPagado(), 0.001);
        assertEquals(15.00, guardada.getSaldoPendiente(), 0.001);
        assertEquals(3, productos.searchById(producto.getId()).getStock());

        assertEquals(15.00,
                new DeudorDaoImpl().buscar("12345678").get(0).getDeuda(), 0.001);

        assertEquals(1, ventas.listAll().size());
    }

    @Test
    void revertirVentaSiNoHayStockSuficiente() throws Exception {

        Producto producto = new Producto("Arroz", 10.00, 1);
        assertTrue(productos.insert(producto));

        Venta venta = new Venta();
        venta.setNombreCliente("Ana Perez");
        venta.setDniCliente(12345678);
        venta.setMontoPagado(0.00);

        venta.agregarDetalle(
                new DetalleVenta(producto.getId(), "Arroz", 2, 10.00));

        assertThrows(java.sql.SQLException.class,
                () -> ventas.registrarVenta(venta));

        assertEquals(1, productos.searchById(producto.getId()).getStock());
        assertTrue(ventas.listAll().isEmpty());
        assertTrue(new DeudorDaoImpl().buscar("12345678").isEmpty());
    }
}