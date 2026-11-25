package pe.edu.upeu.project_final_gp1.implementacion;

import pe.edu.upeu.project_final_gp1.interfaces.IProductoDao;
import pe.edu.upeu.project_final_gp1.modelo.Producto;
import pe.edu.upeu.project_final_gp1.utils.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDaoImpl implements IProductoDao {
    @Override public boolean insert(Producto p) throws SQLException {
        String sql = "INSERT INTO producto(nombre,precio,stock) VALUES(?,?,?)";
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement(sql)) {
            st.setString(1, p.getNombre().trim());
            st.setDouble(2, p.getPrecio());
            st.setInt(3, p.getStock());
            if (st.executeUpdate() == 0) return false;
            try (Statement id = cn.createStatement(); ResultSet rs = id.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) p.setId(rs.getInt(1));
            }
            return p.getId() > 0;
        }
    }

    @Override public boolean update(Producto p) throws SQLException {
        String sql = "UPDATE producto SET nombre=?,precio=?,stock=? WHERE idproducto=?";
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement(sql)) {
            st.setString(1, p.getNombre().trim());
            st.setDouble(2, p.getPrecio());
            st.setInt(3, p.getStock());
            st.setInt(4, p.getId());
            return st.executeUpdate() > 0;
        }
    }

    @Override public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM producto WHERE idproducto=?";

        try (Connection cn = ConexionBD.getConnection()) {
            boolean autoCommit = cn.getAutoCommit();
            cn.setAutoCommit(false);

            try {
                int filas;
                try (PreparedStatement st = cn.prepareStatement(sql)) {
                    st.setInt(1, id);
                    filas = st.executeUpdate();
                }

                if (filas == 0) {
                    cn.rollback();
                    return false;
                }

                try (Statement st = cn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM producto")) {

                    if (rs.next() && rs.getInt(1) == 0) {
                        try (Statement reset = cn.createStatement()) {
                            reset.executeUpdate("DELETE FROM sqlite_sequence WHERE name='producto'");
                        }
                    }
                }
                cn.commit();
                return true;

            } catch (SQLException e) {
                cn.rollback();
                throw e;
            } finally {
                cn.setAutoCommit(autoCommit);
            }
        }
    }

    @Override public Producto searchById(int id) throws SQLException {
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement("SELECT idproducto,nombre,precio,stock FROM producto WHERE idproducto=?")) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    @Override public List<Producto> listAll() throws SQLException {
        List<Producto> result = new ArrayList<>();
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement("SELECT idproducto,nombre,precio,stock FROM producto ORDER BY nombre COLLATE NOCASE"); ResultSet rs = st.executeQuery()) {
            while (rs.next()) result.add(map(rs));
        }
        return result;
    }

    @Override public List<Producto> searchByNombre(String texto) throws SQLException {
        List<Producto> result = new ArrayList<>();
        String q = texto == null ? "" : texto.trim();
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement("SELECT idproducto,nombre,precio,stock FROM producto WHERE CAST(idproducto AS TEXT) LIKE ? OR nombre LIKE ? COLLATE NOCASE ORDER BY nombre COLLATE NOCASE")) {
            st.setString(1, "%" + q + "%");
            st.setString(2, "%" + q + "%");
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) result.add(map(rs)); }
        }
        return result;
    }

    private Producto map(ResultSet rs) throws SQLException {
        return new Producto(rs.getInt("idproducto"), rs.getString("nombre"), rs.getDouble("precio"), rs.getInt("stock"));
    }
}
