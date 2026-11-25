package pe.edu.upeu.project_final_gp1.implementacion;

import pe.edu.upeu.project_final_gp1.interfaces.IDeudorDao;
import pe.edu.upeu.project_final_gp1.modelo.Cliente;
import pe.edu.upeu.project_final_gp1.modelo.Deudor;
import pe.edu.upeu.project_final_gp1.modelo.PagoDeuda;
import pe.edu.upeu.project_final_gp1.utils.ConexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DeudorDaoImpl implements IDeudorDao {
    @Override
    public void registrar(Deudor d) throws SQLException {
        validar(d);
        d.setDeuda(redondear(d.getDeuda()));
        if (d.getDeuda() <= 0) throw new IllegalArgumentException("La deuda inicial debe ser mayor a S/ 0.00.");
        try (Connection cn = ConexionBD.getConnection()) {
            cn.setAutoCommit(false);
            try {
                int idCliente = new ClienteDaoImpl().guardarOActualizar(cn, new Cliente(d.getNombre(), d.getDni()));
                try (PreparedStatement st = cn.prepareStatement("INSERT INTO deudor(idcliente,deuda) VALUES(?,?)")) {
                    st.setInt(1, idCliente);
                    st.setDouble(2, d.getDeuda());
                    st.executeUpdate();
                }
                d.setIdCliente(idCliente);
                cn.commit();
            } catch (Exception e) {
                try {
                    cn.rollback();
                } catch (SQLException ignored) {
                }
                if (e instanceof SQLException se) throw se;
                if (e instanceof RuntimeException re) throw re;
                throw new SQLException("No se pudo guardar el deudor.", e);
            } finally {
                try {
                    cn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public void actualizar(Deudor d) throws SQLException {
        if (d == null || d.getNombre().isBlank()) throw new IllegalArgumentException("El nombre no puede estar vacío.");
        if (!Double.isFinite(d.getDeuda()) || d.getDeuda() < 0)
            throw new IllegalArgumentException("La deuda no puede ser negativa.");
        d.setDeuda(redondear(d.getDeuda()));
        try (Connection cn = ConexionBD.getConnection()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement cliente = cn.prepareStatement("UPDATE cliente SET nombre=? WHERE idcliente=?")) {
                    cliente.setString(1, d.getNombre().trim());
                    cliente.setInt(2, d.getIdCliente());
                    if (cliente.executeUpdate() == 0) throw new SQLException("No se encontró el cliente seleccionado.");
                }
                if (d.getDeuda() <= 0.005) {
                    try (PreparedStatement st = cn.prepareStatement("DELETE FROM deudor WHERE idcliente=?")) {
                        st.setInt(1, d.getIdCliente());
                        st.executeUpdate();
                    }
                } else {
                    try (PreparedStatement st = cn.prepareStatement("UPDATE deudor SET deuda=? WHERE idcliente=?")) {
                        st.setDouble(1, d.getDeuda());
                        st.setInt(2, d.getIdCliente());
                        if (st.executeUpdate() == 0) throw new SQLException("No se encontró el deudor seleccionado.");
                    }
                }
                cn.commit();
            } catch (Exception e) {
                try {
                    cn.rollback();
                } catch (SQLException ignored) {
                }
                if (e instanceof SQLException se) throw se;
                if (e instanceof RuntimeException re) throw re;
                throw new SQLException("No se pudo actualizar el deudor.", e);
            } finally {
                try {
                    cn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public void aumentarDeuda(int idCliente, double monto) throws SQLException {
        if (!Double.isFinite(monto) || monto <= 0)
            throw new IllegalArgumentException("El monto que se agregará a la deuda debe ser mayor a cero.");
        monto = redondear(monto);
        if (monto <= 0) throw new IllegalArgumentException("El monto de la deuda debe ser al menos S/ 0.01.");
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement("UPDATE deudor SET deuda=deuda+? WHERE idcliente=?")) {
            st.setDouble(1, monto);
            st.setInt(2, idCliente);
            if (st.executeUpdate() == 0) throw new SQLException("No se encontró el deudor seleccionado.");
        }
    }

    @Override
    public void registrarPago(int idCliente, double monto) throws SQLException {
        if (!Double.isFinite(monto) || monto <= 0)
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero.");
        monto = redondear(monto);
        if (monto <= 0) throw new IllegalArgumentException("El pago debe ser al menos S/ 0.01.");
        try (Connection cn = ConexionBD.getConnection()) {
            cn.setAutoCommit(false);
            try {
                double deuda;
                try (PreparedStatement q = cn.prepareStatement("SELECT deuda FROM deudor WHERE idcliente=?")) {
                    q.setInt(1, idCliente);
                    try (ResultSet rs = q.executeQuery()) {
                        if (!rs.next()) throw new SQLException("No se encontró el deudor seleccionado.");
                        deuda = rs.getDouble("deuda");
                    }
                }
                if (monto - deuda > 0.005)
                    throw new IllegalArgumentException("El pago no puede superar la deuda actual (S/ " + String.format(java.util.Locale.US, "%.2f", deuda) + ").");
                double nuevaDeuda = redondear(deuda - monto);
                try (PreparedStatement insert = cn.prepareStatement("INSERT INTO cobro_deuda(idcliente,fecha,monto) VALUES(?,?,?)")) {
                    insert.setInt(1, idCliente);
                    insert.setString(2, LocalDate.now().toString());
                    insert.setDouble(3, monto);
                    insert.executeUpdate();
                }
                if (nuevaDeuda <= 0.005) {
                    try (PreparedStatement del = cn.prepareStatement("DELETE FROM deudor WHERE idcliente=?")) {
                        del.setInt(1, idCliente);
                        del.executeUpdate();
                    }
                } else {
                    try (PreparedStatement up = cn.prepareStatement("UPDATE deudor SET deuda=? WHERE idcliente=?")) {
                        up.setDouble(1, nuevaDeuda);
                        up.setInt(2, idCliente);
                        up.executeUpdate();
                    }
                }
                cn.commit();
            } catch (Exception e) {
                try {
                    cn.rollback();
                } catch (SQLException ignored) {
                }
                if (e instanceof SQLException se) throw se;
                if (e instanceof RuntimeException re) throw re;
                throw new SQLException("No se pudo registrar el pago.", e);
            } finally {
                try {
                    cn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public boolean eliminar(int idCliente) throws SQLException {
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement("DELETE FROM deudor WHERE idcliente=?")) {
            st.setInt(1, idCliente);
            return st.executeUpdate() > 0;
        }
    }

    @Override
    public List<Deudor> listAll() throws SQLException {
        return consultar("");
    }

    @Override
    public List<Deudor> buscar(String texto) throws SQLException {
        return consultar(texto == null ? "" : texto.trim());
    }

    @Override
    public List<PagoDeuda> listarPagos() throws SQLException {
        List<PagoDeuda> result = new ArrayList<>();
        String sql = "SELECT p.idcobro,p.idcliente,c.nombre,c.dni,p.fecha,p.monto " +
                "FROM cobro_deuda p INNER JOIN cliente c ON c.idcliente=p.idcliente ORDER BY p.idcobro DESC";

        try (Connection cn = ConexionBD.getConnection();
             PreparedStatement st = cn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            while (rs.next())
                result.add(new PagoDeuda(
                        rs.getInt("idcobro"),
                        rs.getInt("idcliente"),
                        rs.getString("nombre"),
                        rs.getInt("dni"),
                        rs.getString("fecha"),
                        rs.getDouble("monto")
                ));
        }

        return result;
    }

    private List<Deudor> consultar(String texto) throws SQLException {
        List<Deudor> result = new ArrayList<>();
        String sql = "SELECT d.idcliente,c.nombre,c.dni,d.deuda FROM deudor d " +
                "INNER JOIN cliente c ON c.idcliente=d.idcliente " +
                "WHERE c.nombre LIKE ? COLLATE NOCASE OR c.dni LIKE ? COLLATE NOCASE " +
                "ORDER BY c.nombre COLLATE NOCASE";

        try (Connection cn = ConexionBD.getConnection();
             PreparedStatement st = cn.prepareStatement(sql)) {

            st.setString(1, "%" + texto + "%");
            st.setString(2, "%" + texto + "%");

            try (ResultSet rs = st.executeQuery()) {
                while (rs.next())
                    result.add(new Deudor(
                            rs.getInt("idcliente"),
                            rs.getString("nombre"),
                            rs.getInt("dni"),
                            rs.getDouble("deuda")
                    ));
            }
        }

        return result;
    }

    private static double redondear(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void validar(Deudor d) {
        if (d == null || d.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre del deudor es obligatorio.");

        if (d.getDni() <= 0)
            throw new IllegalArgumentException("El DNI del deudor es obligatorio.");

        if (d.getDeuda() < 0 || !Double.isFinite(d.getDeuda()))
            throw new IllegalArgumentException("La deuda no puede ser negativa.");
    }
}