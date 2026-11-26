package pe.edu.upeu.project_final_gp1.test;

import org.junit.jupiter.api.Test;
import pe.edu.upeu.project_final_gp1.implementacion.DeudorDaoImpl;
import pe.edu.upeu.project_final_gp1.modelo.Deudor;

import static org.junit.jupiter.api.Assertions.*;

class DeudorDaoTest extends BaseDaoTest {
    private final DeudorDaoImpl dao = new DeudorDaoImpl();

    @Test
    void registrarAumentarYRecibirPago() throws Exception {

        Deudor nuevo = new Deudor(0, "Carlos Ruiz", 99999997, 50.00);
        dao.registrar(nuevo);

        Deudor deudor = dao.buscar("99999997").get(0);

        assertEquals(50.00, deudor.getDeuda(), 0.001);
        assertEquals(nuevo.getIdCliente(), deudor.getIdCliente());
        assertEquals(99999997, deudor.getDni());

        dao.aumentarDeuda(deudor.getIdCliente(), 10.00);

        assertEquals(60.00, dao.buscar("99999997").get(0).getDeuda(), 0.001);

        dao.registrarPago(deudor.getIdCliente(), 20.00);

        assertEquals(40.00, dao.buscar("99999997").get(0).getDeuda(), 0.001);
    }

    @Test
    void rechazarPagoMayorALaDeuda() throws Exception {

        dao.registrar(new Deudor(0, "Carlos Ruiz", 99999997, 20.00));

        int idCliente = dao.buscar("99999997").get(0).getIdCliente();

        assertThrows(IllegalArgumentException.class,
                () -> dao.registrarPago(idCliente, 30.00));

        assertEquals(20.00, dao.buscar("99999997").get(0).getDeuda(), 0.001);
    }
}