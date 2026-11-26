package pe.edu.upeu.project_final_gp1.implementacion;

import pe.edu.upeu.project_final_gp1.interfaces.IUsuarioDao;
import pe.edu.upeu.project_final_gp1.modelo.Usuario;
import pe.edu.upeu.project_final_gp1.utils.ConexionBD;
import pe.edu.upeu.project_final_gp1.utils.Passwords;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDaoImpl implements IUsuarioDao {
    @Override
    public Usuario login(String user, String password) {
        if (user == null || password == null) return null;
        try (Connection cn = ConexionBD.getConnection()) {
            Usuario us = new Usuario();
            String guardada;
            try (PreparedStatement st = cn.prepareStatement("SELECT idusuario,user,password,nombre,apellidos,tipoUser FROM usuario WHERE user = ?")) {
                st.setString(1, user.trim());
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) return null;
                    guardada = rs.getString("password");
                    us.setIdusuario(rs.getInt("idusuario"));
                    us.setUser(rs.getString("user"));
                    us.setNombre(rs.getString("nombre"));
                    us.setApellidos(rs.getString("apellidos"));
                    us.setTipoUser(rs.getString("tipoUser"));
                }
            }
            if (Passwords.esHash(guardada)) {
                return Passwords.verificar(password, guardada) ? us : null;
            }
            // Base antigua con contraseña en texto plano: se acepta una vez y se reemplaza por su hash.
            if (guardada != null && guardada.equals(password)) {
                actualizarHash(cn, us.getIdusuario(), password);
                return us;
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Error de login: " + e.getMessage());
            return null;
        }
    }

    private void actualizarHash(Connection cn, int id, String password) throws SQLException {
        try (PreparedStatement up = cn.prepareStatement("UPDATE usuario SET password=? WHERE idusuario=?")) {
            up.setString(1, Passwords.hash(password));
            up.setInt(2, id);
            up.executeUpdate();
        }
    }
}
