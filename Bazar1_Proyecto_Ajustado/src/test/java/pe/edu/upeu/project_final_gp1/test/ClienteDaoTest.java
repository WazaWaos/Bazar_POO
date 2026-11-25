package pe.edu.upeu.project_final_gp1.test;

import org.junit.jupiter.api.Test;
import pe.edu.upeu.project_final_gp1.implementacion.ClienteDaoImpl;
import pe.edu.upeu.project_final_gp1.modelo.Cliente;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClienteDaoTest extends BaseDaoTest {
    private final ClienteDaoImpl dao = new ClienteDaoImpl();

    @Test
    void guardarBuscarYActualizarFicha() throws Exception {

        Cliente cliente = new Cliente("Maria Lopez", 12345678);
        int idGuardado = dao.guardarOActualizar(cliente);

        List<Cliente> lista = dao.buscar("Maria");

        assertEquals(1, lista.size());
        assertEquals(idGuardado, lista.get(0).getIdCliente());
        assertEquals(12345678, lista.get(0).getDni());
        assertEquals("Maria Lopez", lista.get(0).getNombre());

        int idActualizado = dao.guardarOActualizar(
                new Cliente("Maria Lopez Diaz", 12345678));

        lista = dao.buscar("12345678");

        assertEquals(1, lista.size());
        assertEquals(idGuardado, idActualizado);
        assertEquals("Maria Lopez Diaz", lista.get(0).getNombre());
        assertEquals(12345678, lista.get(0).getDni());
    }
}