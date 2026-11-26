package pe.edu.upeu.project_final_gp1.interfaces;

import pe.edu.upeu.project_final_gp1.modelo.Cliente;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IClienteDao {
    int guardarOActualizar(Cliente cliente) throws SQLException;
    int guardarOActualizar(Connection cn, Cliente cliente) throws SQLException;
    List<Cliente> buscar(String texto) throws SQLException;
}

