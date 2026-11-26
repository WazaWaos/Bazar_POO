package pe.edu.upeu.project_final_gp1.test;

import org.junit.jupiter.api.Test;
import pe.edu.upeu.project_final_gp1.implementacion.ProductoDaoImpl;
import pe.edu.upeu.project_final_gp1.modelo.Producto;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ProductoDaoTest extends BaseDaoTest {
    private final ProductoDaoImpl dao = new ProductoDaoImpl();

    @Test
    void insertarBuscarActualizarYEliminar() throws Exception {
        Producto producto = new Producto("Arroz", 4.50, 20);
        assertTrue(dao.insert(producto));
        assertTrue(producto.getId() > 0);

        Producto encontrado = dao.searchById(producto.getId());
        assertNotNull(encontrado);
        assertEquals("Arroz", encontrado.getNombre());
        assertEquals(20, encontrado.getStock());

        producto.setNombre("Arroz extra");
        producto.setPrecio(5.00);
        producto.setStock(18);
        assertTrue(dao.update(producto));
        assertEquals("Arroz extra", dao.searchById(producto.getId()).getNombre());
        assertEquals(1, dao.searchByNombre("extra").size());

        assertTrue(dao.delete(producto.getId()));
        assertNull(dao.searchById(producto.getId()));
    }

    @Test
    void noEliminarProductoInexistente() throws Exception {
        assertFalse(dao.delete(999999));
    }
}
