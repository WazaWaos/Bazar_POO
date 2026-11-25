package pe.edu.upeu.project_final_gp1.implementacion;

import pe.edu.upeu.project_final_gp1.interfaces.IDeudorDao;
import pe.edu.upeu.project_final_gp1.modelo.Cliente;
import pe.edu.upeu.project_final_gp1.modelo.Deudor;
import pe.edu.upeu.project_final_gp1.modelo.MovimientoDeuda;
import pe.edu.upeu.project_final_gp1.utils.ConexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Los deudores nunca se borran: cuando el saldo llega a cero el registro queda con estado
 * "Deuda Pagada" y todo su historial (ingresos de deuda y abonos) se conserva.
 * Ingresos de deuda = ventas a crédito del cliente (venta/detalle_venta) + deudas manuales (deudor_ingreso).
 * Pagos = cobro_deuda.
 */
public class DeudorDaoImpl implements IDeudorDao {
    private static final double EPS = 0.005;

    @Override public void registrar(Deudor d, String detalle) throws SQLException {
        validar(d);
        d.setDeuda(redondear(d.getDeuda()));
        if (d.getDeuda() <= 0) throw new IllegalArgumentException("La deuda inicial debe ser mayor a S/ 0.00.");
        try (Connection cn = abrir()) {
            cn.setAutoCommit(false);
            try {
                String dni = d.getDni().trim();
                Integer existente = null; double deudaActual = 0;
                try (PreparedStatement q = cn.prepareStatement("SELECT iddeudor,deuda FROM deudor WHERE dni=? COLLATE NOCASE")) {
                    q.setString(1, dni);
                    try (ResultSet rs = q.executeQuery()) { if (rs.next()) { existente = rs.getInt(1); deudaActual = rs.getDouble(2); } }
                }
                if (existente != null && deudaActual > EPS)
                    throw new IllegalArgumentException("Este cliente ya tiene una deuda pendiente. Selecciónalo en la tabla y usa \"Aumentar deuda\".");
                if (existente == null) {
                    try (PreparedStatement st = cn.prepareStatement("INSERT INTO deudor(nombre,dni,telefono,deuda) VALUES(?,?,?,?)")) {
                        st.setString(1, d.getNombre().trim()); st.setString(2, dni);
                        st.setString(3, d.getTelefono().trim()); st.setDouble(4, d.getDeuda()); st.executeUpdate();
                    }
                } else {
                    // Cliente con deuda pagada: se reutiliza su registro y se conserva el historial.
                    try (PreparedStatement st = cn.prepareStatement("UPDATE deudor SET nombre=?,telefono=?,deuda=? WHERE iddeudor=?")) {
                        st.setString(1, d.getNombre().trim()); st.setString(2, d.getTelefono().trim());
                        st.setDouble(3, d.getDeuda()); st.setInt(4, existente); st.executeUpdate();
                    }
                }
                insertarIngreso(cn, dni, d.getDeuda(), detalle == null || detalle.isBlank() ? "Deuda inicial" : detalle);
                new ClienteDaoImpl().guardarOActualizar(cn, new Cliente(d.getNombre(), d.getDni(), d.getTelefono()));
                cn.commit();
            } catch (Exception e) { revertir(cn, e, "No se pudo guardar el deudor."); }
            finally { try { cn.setAutoCommit(true); } catch (SQLException ignored) {} }
        }
    }

    @Override public void actualizar(Deudor d) throws SQLException {
        if (d.getNombre().isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío.");
        try (Connection cn = abrir()) {
            cn.setAutoCommit(false);
            try {
                String dni;
                try (PreparedStatement q = cn.prepareStatement("SELECT dni FROM deudor WHERE iddeudor=?")) {
                    q.setInt(1, d.getIdDeudor());
                    try (ResultSet rs = q.executeQuery()) {
                        if (!rs.next()) throw new SQLException("El deudor ya no existe.");
                        dni = rs.getString(1);
                    }
                }
                // El saldo no se edita a mano: depende de los ingresos y pagos registrados.
                try (PreparedStatement st = cn.prepareStatement("UPDATE deudor SET nombre=?,telefono=? WHERE iddeudor=?")) {
                    st.setString(1, d.getNombre().trim()); st.setString(2, d.getTelefono().trim()); st.setInt(3, d.getIdDeudor());
                    st.executeUpdate();
                }
                new ClienteDaoImpl().guardarOActualizar(cn, new Cliente(d.getNombre(), dni, d.getTelefono()));
                cn.commit();
            } catch (Exception e) { revertir(cn, e, "No se pudo actualizar el deudor."); }
            finally { try { cn.setAutoCommit(true); } catch (SQLException ignored) {} }
        }
    }

    @Override public void aumentarDeuda(int id, double monto, String detalle) throws SQLException {
        if (!Double.isFinite(monto) || monto <= 0) throw new IllegalArgumentException("El monto que se agregará a la deuda debe ser mayor a cero.");
        monto = redondear(monto);
        if (monto <= 0) throw new IllegalArgumentException("El monto de la deuda debe ser al menos S/ 0.01.");
        try (Connection cn = abrir()) {
            cn.setAutoCommit(false);
            try {
                String dni = dniDe(cn, id);
                try (PreparedStatement st = cn.prepareStatement("UPDATE deudor SET deuda=deuda+? WHERE iddeudor=?")) {
                    st.setDouble(1, monto); st.setInt(2, id); st.executeUpdate();
                }
                insertarIngreso(cn, dni, monto, detalle);
                cn.commit();
            } catch (Exception e) { revertir(cn, e, "No se pudo aumentar la deuda."); }
            finally { try { cn.setAutoCommit(true); } catch (SQLException ignored) {} }
        }
    }

    @Override public void registrarPago(int id, double monto) throws SQLException {
        if (!Double.isFinite(monto) || monto <= 0) throw new IllegalArgumentException("El monto del pago debe ser mayor a cero.");
        monto = redondear(monto);
        if (monto <= 0) throw new IllegalArgumentException("El pago debe ser al menos S/ 0.01.");
        try (Connection cn = abrir()) {
            cn.setAutoCommit(false);
            try {
                String nombre; String dni; double deuda;
                try (PreparedStatement q = cn.prepareStatement("SELECT nombre,dni,deuda FROM deudor WHERE iddeudor=?")) {
                    q.setInt(1, id);
                    try (ResultSet rs = q.executeQuery()) {
                        if (!rs.next()) throw new SQLException("No se encontró el deudor seleccionado.");
                        nombre = rs.getString("nombre"); dni = rs.getString("dni"); deuda = rs.getDouble("deuda");
                    }
                }
                if (deuda <= EPS) throw new IllegalArgumentException("Este cliente no tiene deuda pendiente (Deuda Pagada).");
                if (monto - deuda > EPS) throw new IllegalArgumentException("El pago no puede superar la deuda actual (S/ " + String.format(java.util.Locale.US, "%.2f", deuda) + ").");
                double nuevaDeuda = redondear(deuda - monto);
                if (nuevaDeuda <= EPS) nuevaDeuda = 0;
                try (PreparedStatement insert = cn.prepareStatement("INSERT INTO cobro_deuda(nombre_cliente,dni_cliente,fecha,monto) VALUES(?,?,?,?)")) {
                    insert.setString(1, nombre); insert.setString(2, dni); insert.setString(3, LocalDate.now().toString()); insert.setDouble(4, monto); insert.executeUpdate();
                }
                // Nunca se borra el registro: con saldo cero el estado pasa a "Deuda Pagada".
                try (PreparedStatement up = cn.prepareStatement("UPDATE deudor SET deuda=? WHERE iddeudor=?")) { up.setDouble(1, nuevaDeuda); up.setInt(2, id); up.executeUpdate(); }
                cn.commit();
            } catch (Exception e) { revertir(cn, e, "No se pudo registrar el pago."); }
            finally { try { cn.setAutoCommit(true); } catch (SQLException ignored) {} }
        }
    }

    @Override public boolean eliminar(int id) throws SQLException {
        throw new UnsupportedOperationException("Los deudores no se eliminan: al pagar la deuda quedan con estado \"Deuda Pagada\" para conservar el historial.");
    }

    @Override public List<Deudor> listAll() throws SQLException { return consultar(""); }
    @Override public List<Deudor> buscar(String texto) throws SQLException { return consultar(texto == null ? "" : texto.trim()); }

    @Override public List<MovimientoDeuda> listarIngresos(int id) throws SQLException {
        String sql = "SELECT fecha,detalle,monto FROM (" +
                " SELECT v.fecha AS fecha, COALESCE((SELECT group_concat(dv.nombre_producto || ' x' || dv.cantidad, ', ') FROM detalle_venta dv WHERE dv.idventa=v.idventa),'') AS detalle," +
                "        v.saldo_pendiente AS monto, 0 AS origen, v.idventa AS orden" +
                " FROM venta v WHERE trim(v.cliente_dni)=? COLLATE NOCASE AND v.saldo_pendiente>0.005" +
                " UNION ALL" +
                " SELECT i.fecha, i.detalle, i.monto, 1, i.idingreso FROM deudor_ingreso i WHERE i.dni=? COLLATE NOCASE" +
                ") ORDER BY fecha, origen, orden";
        List<MovimientoDeuda> lista = new ArrayList<>();
        try (Connection cn = abrir()) {
            String dni = dniDe(cn, id);
            try (PreparedStatement st = cn.prepareStatement(sql)) {
                st.setString(1, dni); st.setString(2, dni);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        String detalle = rs.getString("detalle");
                        lista.add(new MovimientoDeuda(rs.getString("fecha"), detalle == null || detalle.isBlank() ? "Sin detalle registrado" : detalle, rs.getDouble("monto")));
                    }
                }
            }
        }
        return lista;
    }

    @Override public List<MovimientoDeuda> listarPagos(int id) throws SQLException {
        List<MovimientoDeuda> lista = new ArrayList<>();
        try (Connection cn = abrir()) {
            String dni = dniDe(cn, id);
            try (PreparedStatement st = cn.prepareStatement("SELECT fecha,monto FROM cobro_deuda WHERE trim(dni_cliente)=? COLLATE NOCASE ORDER BY fecha,idcobro")) {
                st.setString(1, dni);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) lista.add(new MovimientoDeuda(rs.getString("fecha"), "Abono", rs.getDouble("monto")));
                }
            }
        }
        return lista;
    }

    private List<Deudor> consultar(String texto) throws SQLException {
        List<Deudor> result = new ArrayList<>();
        // Primero los que aún deben y después los que ya pagaron.
        String sql = "SELECT iddeudor,nombre,dni,telefono,deuda FROM deudor WHERE nombre LIKE ? COLLATE NOCASE OR dni LIKE ? COLLATE NOCASE " +
                "ORDER BY CASE WHEN deuda>0.005 THEN 0 ELSE 1 END, nombre COLLATE NOCASE";
        try (Connection cn = abrir(); PreparedStatement st = cn.prepareStatement(sql)) {
            st.setString(1, "%" + texto + "%"); st.setString(2, "%" + texto + "%");
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) result.add(new Deudor(rs.getInt("iddeudor"), rs.getString("nombre"), rs.getString("dni"), rs.getString("telefono"), rs.getDouble("deuda")));
            }
        }
        return result;
    }

    /** Abre la conexión y deja listo el historial de ingresos de deuda (sin tocar ConexionBD). */
    private Connection abrir() throws SQLException {
        Connection cn = ConexionBD.getConnection();
        try { prepararHistorial(cn); return cn; }
        catch (SQLException | RuntimeException e) { try { cn.close(); } catch (SQLException ignored) {} throw e; }
    }

    private void prepararHistorial(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS deudor_ingreso (idingreso INTEGER PRIMARY KEY AUTOINCREMENT, dni TEXT NOT NULL COLLATE NOCASE, " +
                    "fecha TEXT NOT NULL, detalle TEXT NOT NULL DEFAULT '', monto REAL NOT NULL, FOREIGN KEY(dni) REFERENCES deudor(dni) ON DELETE CASCADE)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_deudor_ingreso_dni ON deudor_ingreso(dni)");
        }
        // Datos anteriores a este cambio: cuadra el historial con el saldo guardado para que ingresos - pagos = deuda.
        String sql = "SELECT d.dni, d.deuda - (" +
                " COALESCE((SELECT SUM(v.saldo_pendiente) FROM venta v WHERE trim(v.cliente_dni)=d.dni COLLATE NOCASE AND v.saldo_pendiente>0.005),0)" +
                " + COALESCE((SELECT SUM(i.monto) FROM deudor_ingreso i WHERE i.dni=d.dni),0)" +
                " - COALESCE((SELECT SUM(c.monto) FROM cobro_deuda c WHERE trim(c.dni_cliente)=d.dni COLLATE NOCASE),0)) FROM deudor d";
        List<String> dnis = new ArrayList<>(); List<Double> difs = new ArrayList<>();
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                double dif = redondear(rs.getDouble(2));
                if (Math.abs(dif) > EPS) { dnis.add(rs.getString(1)); difs.add(dif); }
            }
        }
        for (int i = 0; i < dnis.size(); i++)
            insertarIngreso(cn, dnis.get(i), difs.get(i), difs.get(i) > 0 ? "Saldo anterior (registro previo al historial)" : "Ajuste de saldo anterior");
    }

    private void insertarIngreso(Connection cn, String dni, double monto, String detalle) throws SQLException {
        try (PreparedStatement st = cn.prepareStatement("INSERT INTO deudor_ingreso(dni,fecha,detalle,monto) VALUES(?,?,?,?)")) {
            st.setString(1, dni); st.setString(2, LocalDate.now().toString());
            st.setString(3, detalle == null || detalle.isBlank() ? "Sin detalle registrado" : detalle.trim());
            st.setDouble(4, monto); st.executeUpdate();
        }
    }

    private String dniDe(Connection cn, int id) throws SQLException {
        try (PreparedStatement q = cn.prepareStatement("SELECT dni FROM deudor WHERE iddeudor=?")) {
            q.setInt(1, id);
            try (ResultSet rs = q.executeQuery()) {
                if (!rs.next()) throw new SQLException("No se encontró el deudor seleccionado.");
                return rs.getString(1);
            }
        }
    }

    private void revertir(Connection cn, Exception e, String mensaje) throws SQLException {
        try { cn.rollback(); } catch (SQLException ignored) {}
        if (e instanceof SQLException se) throw se;
        if (e instanceof RuntimeException re) throw re;
        throw new SQLException(mensaje, e);
    }

    private static double redondear(double value) { return Math.round(value * 100.0) / 100.0; }

    private void validar(Deudor d) {
        if (d == null || d.getNombre().isBlank()) throw new IllegalArgumentException("El nombre del deudor es obligatorio.");
        if (d.getDni().isBlank()) throw new IllegalArgumentException("El DNI del deudor es obligatorio.");
        if (d.getDeuda() < 0 || !Double.isFinite(d.getDeuda())) throw new IllegalArgumentException("La deuda no puede ser negativa.");
    }
}
