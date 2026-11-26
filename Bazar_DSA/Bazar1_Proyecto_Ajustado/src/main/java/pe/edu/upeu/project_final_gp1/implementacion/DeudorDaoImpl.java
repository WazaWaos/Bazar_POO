package pe.edu.upeu.project_final_gp1.implementacion;

import pe.edu.upeu.project_final_gp1.interfaces.IDeudorDao;
import pe.edu.upeu.project_final_gp1.modelo.Cliente;
import pe.edu.upeu.project_final_gp1.modelo.Deudor;
import pe.edu.upeu.project_final_gp1.modelo.MovimientoDeuda;
import pe.edu.upeu.project_final_gp1.utils.ConexionBD;
import pe.edu.upeu.project_final_gp1.utils.Dni;
import pe.edu.upeu.project_final_gp1.utils.Sql;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Un deudor es un cliente con saldo: {@code Deudor.idDeudor} es el {@code idcliente}.
 * Nunca se borra: al llegar a cero queda como "Deuda Pagada" y conserva su historial.
 *
 * <p>Saldo = ingresos de deuda - pagos, donde los ingresos son las ventas a crédito del cliente
 * (tabla venta) más las deudas manuales (deudor_ingreso) y los pagos son cobro_deuda.</p>
 */
public class DeudorDaoImpl implements IDeudorDao {
    private static final double EPS = 0.005;

    @Override public void registrar(Deudor d, String detalle) throws SQLException {
        validar(d);
        d.setDeuda(redondear(d.getDeuda()));
        if (d.getDeuda() <= 0) throw new IllegalArgumentException("La deuda inicial debe ser mayor a S/ 0.00.");
        try (Connection cn = ConexionBD.getConnection()) {
            cn.setAutoCommit(false);
            try {
                int idCliente = new ClienteDaoImpl().guardarOActualizar(cn, new Cliente(d.getNombre(), d.getDni(), d.getTelefono()));
                Double actual = deudaDe(cn, idCliente);
                if (actual != null && actual > EPS)
                    throw new IllegalArgumentException("Este cliente ya tiene una deuda pendiente. Selecciónalo en la tabla y usa \"Aumentar deuda\".");
                try (PreparedStatement st = cn.prepareStatement(actual == null
                        ? "INSERT INTO deudor(deuda,idcliente) VALUES(?,?)"
                        : "UPDATE deudor SET deuda=? WHERE idcliente=?")) { // cliente con deuda pagada: se reutiliza su registro
                    st.setDouble(1, d.getDeuda()); st.setInt(2, idCliente); st.executeUpdate();
                }
                insertarIngreso(cn, idCliente, d.getDeuda(), detalle == null || detalle.isBlank() ? "Deuda inicial" : detalle);
                cn.commit();
                d.setIdDeudor(idCliente);
            } catch (Exception e) { revertir(cn, e, "No se pudo guardar el deudor."); }
            finally { restaurar(cn); }
        }
    }

    @Override public void actualizar(Deudor d) throws SQLException {
        if (d == null || d.getNombre().isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío.");
        try (Connection cn = ConexionBD.getConnection()) {
            cn.setAutoCommit(false);
            try {
                if (deudaDe(cn, d.getIdDeudor()) == null) throw new SQLException("El deudor ya no existe.");
                // Solo datos de contacto: el saldo se calcula con los ingresos y pagos registrados.
                try (PreparedStatement st = cn.prepareStatement("UPDATE cliente SET nombre=?, telefono=? WHERE idcliente=?")) {
                    st.setString(1, d.getNombre().trim()); st.setString(2, d.getTelefono().trim()); st.setInt(3, d.getIdDeudor());
                    st.executeUpdate();
                }
                cn.commit();
            } catch (Exception e) { revertir(cn, e, "No se pudo actualizar el deudor."); }
            finally { restaurar(cn); }
        }
    }

    @Override public void aumentarDeuda(int id, double monto, String detalle) throws SQLException {
        if (!Double.isFinite(monto) || monto <= 0) throw new IllegalArgumentException("El monto que se agregará a la deuda debe ser mayor a cero.");
        monto = redondear(monto);
        if (monto <= 0) throw new IllegalArgumentException("El monto de la deuda debe ser al menos S/ 0.01.");
        try (Connection cn = ConexionBD.getConnection()) {
            cn.setAutoCommit(false);
            try {
                if (deudaDe(cn, id) == null) throw new SQLException("No se encontró el deudor seleccionado.");
                try (PreparedStatement st = cn.prepareStatement("UPDATE deudor SET deuda=ROUND(deuda+?,2) WHERE idcliente=?")) {
                    st.setDouble(1, monto); st.setInt(2, id); st.executeUpdate();
                }
                insertarIngreso(cn, id, monto, detalle);
                cn.commit();
            } catch (Exception e) { revertir(cn, e, "No se pudo aumentar la deuda."); }
            finally { restaurar(cn); }
        }
    }

    @Override public void registrarPago(int id, double monto) throws SQLException {
        if (!Double.isFinite(monto) || monto <= 0) throw new IllegalArgumentException("El monto del pago debe ser mayor a cero.");
        monto = redondear(monto);
        if (monto <= 0) throw new IllegalArgumentException("El pago debe ser al menos S/ 0.01.");
        try (Connection cn = ConexionBD.getConnection()) {
            cn.setAutoCommit(false);
            try {
                Double deuda = deudaDe(cn, id);
                if (deuda == null) throw new SQLException("No se encontró el deudor seleccionado.");
                if (deuda <= EPS) throw new IllegalArgumentException("Este cliente no tiene deuda pendiente (Deuda Pagada).");
                if (monto - deuda > EPS) throw new IllegalArgumentException("El pago no puede superar la deuda actual (S/ " + String.format(java.util.Locale.US, "%.2f", deuda) + ").");
                double nueva = redondear(deuda - monto);
                if (nueva <= EPS) nueva = 0;
                try (PreparedStatement st = cn.prepareStatement("INSERT INTO cobro_deuda(idcliente,fecha,monto) VALUES(?,?,?)")) {
                    st.setInt(1, id); st.setString(2, LocalDate.now().toString()); st.setDouble(3, monto); st.executeUpdate();
                }
                try (PreparedStatement st = cn.prepareStatement("UPDATE deudor SET deuda=? WHERE idcliente=?")) {
                    st.setDouble(1, nueva); st.setInt(2, id); st.executeUpdate();
                }
                cn.commit();
            } catch (Exception e) { revertir(cn, e, "No se pudo registrar el pago."); }
            finally { restaurar(cn); }
        }
    }

    @Override public boolean eliminar(int id) {
        throw new UnsupportedOperationException("Los deudores no se eliminan: al pagar la deuda quedan con estado \"Deuda Pagada\" para conservar el historial.");
    }

    @Override public List<Deudor> listAll() throws SQLException { return consultar(""); }
    @Override public List<Deudor> buscar(String texto) throws SQLException { return consultar(texto == null ? "" : texto.trim()); }

    @Override public List<MovimientoDeuda> listarIngresos(int id) throws SQLException {
        String sql = "SELECT fecha,detalle,monto FROM (" +
                " SELECT v.fecha AS fecha, COALESCE((SELECT group_concat(p.nombre || ' x' || dv.cantidad, ', ') FROM detalle_venta dv JOIN producto p ON p.idproducto=dv.idproducto WHERE dv.idventa=v.idventa),'') AS detalle," +
                "        v.saldo_pendiente AS monto, 0 AS origen, v.idventa AS orden" +
                " FROM venta v WHERE v.idcliente=? AND v.saldo_pendiente>0.005" +
                " UNION ALL" +
                " SELECT i.fecha, i.detalle, i.monto, 1, i.idingreso FROM deudor_ingreso i WHERE i.idcliente=?" +
                ") ORDER BY fecha, origen, orden";
        List<MovimientoDeuda> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement(sql)) {
            st.setInt(1, id); st.setInt(2, id);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String detalle = rs.getString("detalle");
                    lista.add(new MovimientoDeuda(rs.getString("fecha"), detalle == null || detalle.isBlank() ? "Sin detalle registrado" : detalle, rs.getDouble("monto")));
                }
            }
        }
        return lista;
    }

    @Override public List<MovimientoDeuda> listarPagos(int id) throws SQLException {
        List<MovimientoDeuda> lista = new ArrayList<>();
        try (Connection cn = ConexionBD.getConnection();
             PreparedStatement st = cn.prepareStatement("SELECT fecha,monto FROM cobro_deuda WHERE idcliente=? ORDER BY fecha,idcobro")) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) lista.add(new MovimientoDeuda(rs.getString("fecha"), "Abono", rs.getDouble("monto")));
            }
        }
        return lista;
    }

    // ------------------------------------------------------------------ internos

    private List<Deudor> consultar(String texto) throws SQLException {
        List<Deudor> result = new ArrayList<>();
        String like = Sql.contiene(texto);
        // Primero los que aún deben y después los que ya pagaron.
        String sql = "SELECT c.idcliente,c.nombre,c.dni,c.telefono,d.deuda FROM deudor d JOIN cliente c ON c.idcliente=d.idcliente " +
                "WHERE c.nombre LIKE ? ESCAPE '\\' COLLATE NOCASE OR printf('%08d', c.dni) LIKE ? ESCAPE '\\' " +
                "ORDER BY CASE WHEN d.deuda>0.005 THEN 0 ELSE 1 END, c.nombre COLLATE NOCASE";
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement(sql)) {
            st.setString(1, like); st.setString(2, like);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) result.add(new Deudor(rs.getInt("idcliente"), rs.getString("nombre"), rs.getInt("dni"), rs.getString("telefono"), rs.getDouble("deuda")));
            }
        }
        return result;
    }

    /** Saldo actual del deudor, o null si el cliente no está registrado como deudor. */
    private Double deudaDe(Connection cn, int idCliente) throws SQLException {
        try (PreparedStatement q = cn.prepareStatement("SELECT deuda FROM deudor WHERE idcliente=?")) {
            q.setInt(1, idCliente);
            try (ResultSet rs = q.executeQuery()) { return rs.next() ? rs.getDouble(1) : null; }
        }
    }

    private void insertarIngreso(Connection cn, int idCliente, double monto, String detalle) throws SQLException {
        try (PreparedStatement st = cn.prepareStatement("INSERT INTO deudor_ingreso(idcliente,fecha,detalle,monto) VALUES(?,?,?,?)")) {
            st.setInt(1, idCliente); st.setString(2, LocalDate.now().toString());
            st.setString(3, detalle == null || detalle.isBlank() ? "Sin detalle registrado" : detalle.trim());
            st.setDouble(4, monto); st.executeUpdate();
        }
    }

    private void revertir(Connection cn, Exception e, String mensaje) throws SQLException {
        try { cn.rollback(); } catch (SQLException ignored) { }
        if (e instanceof SQLException se) throw se;
        if (e instanceof RuntimeException re) throw re;
        throw new SQLException(mensaje, e);
    }

    private void restaurar(Connection cn) { try { cn.setAutoCommit(true); } catch (SQLException ignored) { } }

    private static double redondear(double value) { return Math.round(value * 100.0) / 100.0; }

    private void validar(Deudor d) {
        if (d == null || d.getNombre().isBlank()) throw new IllegalArgumentException("El nombre del deudor es obligatorio.");
        Dni.validar(d.getDni());
        if (d.getDeuda() < 0 || !Double.isFinite(d.getDeuda())) throw new IllegalArgumentException("La deuda no puede ser negativa.");
    }
}
