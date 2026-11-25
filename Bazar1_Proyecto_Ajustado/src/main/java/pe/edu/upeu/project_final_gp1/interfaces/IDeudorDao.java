package pe.edu.upeu.project_final_gp1.interfaces;

import pe.edu.upeu.project_final_gp1.modelo.Deudor;
import pe.edu.upeu.project_final_gp1.modelo.PagoDeuda;

import java.sql.SQLException;
import java.util.List;

public interface IDeudorDao {
    void registrar(Deudor deudor) throws SQLException;
    void actualizar(Deudor deudor) throws SQLException;
    void aumentarDeuda(int idCliente, double monto) throws SQLException;
    void registrarPago(int idCliente, double monto) throws SQLException;
    boolean eliminar(int idCliente) throws SQLException;
    List<Deudor> listAll() throws SQLException;
    List<Deudor> buscar(String texto) throws SQLException;
    List<PagoDeuda> listarPagos() throws SQLException;
}
