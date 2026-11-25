package pe.edu.upeu.project_final_gp1.interfaces;

import pe.edu.upeu.project_final_gp1.modelo.Deudor;
import pe.edu.upeu.project_final_gp1.modelo.MovimientoDeuda;

import java.sql.SQLException;
import java.util.List;

public interface IDeudorDao {
    default void registrar(Deudor deudor) throws SQLException { registrar(deudor, ""); }
    /** Registra al cliente con su deuda inicial y el detalle de los productos que la originaron. */
    void registrar(Deudor deudor, String detalle) throws SQLException;
    /** Actualiza solo los datos de contacto; el saldo se calcula con ingresos y pagos. */
    void actualizar(Deudor deudor) throws SQLException;
    default void aumentarDeuda(int idDeudor, double monto) throws SQLException { aumentarDeuda(idDeudor, monto, ""); }
    /** Agrega una deuda al cliente indicando el detalle de los productos que la originaron. */
    void aumentarDeuda(int idDeudor, double monto, String detalle) throws SQLException;
    /** Registra un abono. Al llegar a cero el registro se conserva con estado "Deuda Pagada". */
    void registrarPago(int idDeudor, double monto) throws SQLException;
    /** Ya no se borra físicamente a un deudor con historial; se rechaza la operación. */
    boolean eliminar(int idDeudor) throws SQLException;
    List<Deudor> listAll() throws SQLException;
    List<Deudor> buscar(String texto) throws SQLException;
    /** Tabla 1: deudas generadas (fecha, productos, monto). */
    List<MovimientoDeuda> listarIngresos(int idDeudor) throws SQLException;
    /** Tabla 2: abonos realizados (fecha, monto). */
    List<MovimientoDeuda> listarPagos(int idDeudor) throws SQLException;
}
