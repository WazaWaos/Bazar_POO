package pe.edu.upeu.project_final_gp1.implementacion;

import pe.edu.upeu.project_final_gp1.interfaces.IClienteDao;
import pe.edu.upeu.project_final_gp1.modelo.Cliente;
import pe.edu.upeu.project_final_gp1.utils.ConexionBD;
import pe.edu.upeu.project_final_gp1.utils.Dni;
import pe.edu.upeu.project_final_gp1.utils.Sql;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDaoImpl implements IClienteDao {
    @Override
    public List<Cliente> buscar(String termino) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        if (termino == null || termino.isBlank()) return clientes;

        String like = Sql.contiene(termino);
        // El DNI se compara con su forma de 8 dígitos para que "0765" encuentre al DNI 07654321.
        String sql = "SELECT idcliente,nombre,dni,telefono FROM cliente " +
                "WHERE nombre LIKE ? ESCAPE '\\' COLLATE NOCASE OR printf('%08d', dni) LIKE ? ESCAPE '\\' " +
                "ORDER BY nombre COLLATE NOCASE LIMIT 10";
        try (Connection cn = ConexionBD.getConnection(); PreparedStatement st = cn.prepareStatement(sql)) {
            st.setString(1, like);
            st.setString(2, like);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next())
                    clientes.add(new Cliente(rs.getInt("idcliente"), rs.getString("nombre"), rs.getInt("dni"), rs.getString("telefono")));
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

        try (PreparedStatement st = cn.prepareStatement("SELECT idcliente FROM cliente WHERE dni=?")) {
            st.setInt(1, cliente.getDni());
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("idcliente");
                    // El teléfono solo se reemplaza si se escribió uno nuevo (no se borra al guardar una venta).
                    try (PreparedStatement up = cn.prepareStatement(
                            "UPDATE cliente SET nombre=?, telefono=CASE WHEN ?='' THEN telefono ELSE ? END WHERE idcliente=?")) {
                        up.setString(1, cliente.getNombre());
                        up.setString(2, cliente.getTelefono());
                        up.setString(3, cliente.getTelefono());
                        up.setInt(4, id);
                        up.executeUpdate();
                    }
                    cliente.setIdCliente(id);
                    return id;
                }
            }
        }

        try (PreparedStatement insert = cn.prepareStatement("INSERT INTO cliente(nombre,dni,telefono) VALUES(?,?,?)")) {
            insert.setString(1, cliente.getNombre());
            insert.setInt(2, cliente.getDni());
            insert.setString(3, cliente.getTelefono());
            insert.executeUpdate();
        }
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) {
                cliente.setIdCliente(rs.getInt(1));
                return cliente.getIdCliente();
            }
        }
        throw new SQLException("No se pudo obtener el ID del cliente.");
    }

    private void validar(Cliente cliente) {
        if (cliente == null) throw new IllegalArgumentException("La ficha del cliente es obligatoria.");
        if (cliente.getNombre().isBlank()) throw new IllegalArgumentException("Ingresa el nombre del cliente.");
        Dni.validar(cliente.getDni());
    }
}
