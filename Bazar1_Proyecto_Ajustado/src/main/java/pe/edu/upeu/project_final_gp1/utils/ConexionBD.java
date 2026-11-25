package pe.edu.upeu.project_final_gp1.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class ConexionBD {
    private static Connection connection;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                String url = "jdbc:sqlite:C:/sqlite/db/bazar1.db";
                if (url == null || url.isBlank()) {
                    throw new SQLException("Agrega la ruta de la base de datos antes de usar el sistema.");
                }
                if (!url.startsWith("jdbc:sqlite:")) throw new SQLException("La conexión debe utilizar SQLite.");
                String ruta = url.substring("jdbc:sqlite:".length());
                if (ruta.isBlank()) throw new SQLException("La ruta de la base de datos está vacía.");
                if (!":memory:".equals(ruta) && !Files.isRegularFile(Path.of(ruta))) {
                    throw new SQLException("No se encontró la base de datos: " + ruta);
                }
                connection = DriverManager.getConnection(url);
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON;");
                }
                System.out.println("Conexion exitosa a SQLite");
            }
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Error de conexion a la BD: " + e.getMessage(), e);
        }
    }
}
