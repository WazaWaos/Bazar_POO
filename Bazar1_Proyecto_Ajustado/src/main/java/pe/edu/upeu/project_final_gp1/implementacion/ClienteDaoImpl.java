package pe.edu.upeu.project_final_gp1.implementacion;

import pe.edu.upeu.project_final_gp1.interfaces.IClienteDao;
import pe.edu.upeu.project_final_gp1.modelo.Cliente;
import pe.edu.upeu.project_final_gp1.utils.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDaoImpl implements IClienteDao {
    @Override
    public List<Cliente> buscar(String termino) throws SQLException {
        String filtro = termino == null ? "" : termino.trim();
        List<Cliente> clientes = new ArrayList<>();
        if (filtro.isBlank()) return clientes;

        String sql = "SELECT idcliente,nombre,dni FROM cliente " +
                "WHERE nombre LIKE ? COLLATE NOCASE OR dni LIKE ? COLLATE NOCASE " +
                "ORDER BY nombre COLLATE NOCASE LIMIT 10";

        try (Connection cn = ConexionBD.getConnection();
             PreparedStatement st = cn.prepareStatement(sql)) {

            st.setString(1, "%" + filtro + "%");
            st.setString(2, "%" + filtro + "%");

            try (ResultSet rs = st.executeQuery()) {
                while (rs.next())
                    clientes.add(new Cliente(
                            rs.getInt("idcliente"),
                            rs.getString("nombre"),
                            rs.getInt("dni")
                    ));
            }
        }

        return clientes;
    }

    @Override
    public int guardarOActualizar(Cliente cliente) throws SQLException {
        try (Connection cn = ConexionBD.getConnection()) {
            return guardarOActualizar(cn, cliente);
        }
    }

    @Override
    public int guardarOActualizar(Connection cn, Cliente cliente) throws SQLException {
        validar(cliente);

        String buscar = "SELECT idcliente FROM cliente WHERE dni=?";

        try (PreparedStatement st = cn.prepareStatement(buscar)) {
            st.setInt(1, cliente.getDni());

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("idcliente");

                    try (PreparedStatement update = cn.prepareStatement("UPDATE cliente SET nombre=? WHERE idcliente=?")) {
                        update.setString(1, cliente.getNombre());
                        update.setInt(2, id);
                        update.executeUpdate();
                    }

                    cliente.setIdCliente(id);
                    return id;
                }
            }
        }

        try (PreparedStatement insert = cn.prepareStatement("INSERT INTO cliente(nombre,dni) VALUES(?,?)")) {
            insert.setString(1, cliente.getNombre());
            insert.setInt(2, cliente.getDni());
            insert.executeUpdate();
        }

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {

            if (rs.next()) {
                int id = rs.getInt(1);
                cliente.setIdCliente(id);
                return id;
            }
        }

        throw new SQLException("No se pudo obtener el ID del cliente.");
    }

    private void validar(Cliente cliente) {
        if (cliente == null)
            throw new IllegalArgumentException("La ficha del cliente es obligatoria.");

        if (cliente.getNombre().isBlank())
            throw new IllegalArgumentException("Ingresa el nombre del cliente.");

        if (cliente.getDni() <= 0)
            throw new IllegalArgumentException("Ingresa un DNI válido.");
    }
}
