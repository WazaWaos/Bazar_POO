package pe.edu.upeu.project_final_gp1.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a la base SQLite de Bazar 1.
 *
 * <p>Ruta de la base (en este orden): propiedad {@code -Dapp.db.path=...}, variable de entorno
 * {@code BAZAR1_DB}, o {@code data/bazar1.db} dentro del directorio de ejecución.
 * Si el archivo no existe se crea con el esquema de {@code schema.sql}; si es del esquema antiguo
 * se hace una copia de seguridad y se migra automáticamente.</p>
 */
public final class ConexionBD {
    private static final String RECURSO_ESQUEMA = "/pe/edu/upeu/project_final_gp1/schema.sql";
    private static final String USUARIO_INICIAL = "admin";
    private static final String CLAVE_INICIAL = "admin123";

    private static Path inicializada;

    private ConexionBD() {}

    /** Abre una conexión nueva. Quien la recibe debe cerrarla (try-with-resources). */
    public static synchronized Connection getConnection() throws SQLException {
        Path path = getDatabasePath();
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Class.forName("org.sqlite.JDBC");
        } catch (IOException | ClassNotFoundException e) {
            throw new SQLException("No se pudo preparar la base de datos en " + path + ": " + e.getMessage(), e);
        }
        Connection cn = DriverManager.getConnection("jdbc:sqlite:" + path);
        try {
            try (Statement st = cn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("PRAGMA busy_timeout = 5000");
            }
            if (!path.equals(inicializada)) {
                inicializar(cn, path);
                inicializada = path;
                System.out.println("Base de datos Bazar 1: " + path);
            }
            return cn;
        } catch (SQLException | RuntimeException e) {
            try { cn.close(); } catch (SQLException ignored) { }
            throw e;
        }
    }

    public static synchronized Path getDatabasePath() {
        String configurada = System.getProperty("app.db.path");
        if (configurada == null || configurada.isBlank()) configurada = System.getenv("BAZAR1_DB");
        if (configurada != null && !configurada.isBlank()) return Path.of(configurada).toAbsolutePath().normalize();
        return Path.of(System.getProperty("user.dir"), "data", "bazar1.db").toAbsolutePath().normalize();
    }

    // ------------------------------------------------------------------ inicialización

    private static void inicializar(Connection cn, Path path) throws SQLException {
        if (MigracionLegacy.necesita(cn)) {
            Path copia = path.resolveSibling(path.getFileName() + ".respaldo-esquema-antiguo");
            try {
                Files.copy(path, copia, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new SQLException("No se pudo crear la copia de seguridad antes de migrar: " + e.getMessage(), e);
            }
            System.out.println("Esquema antiguo detectado. Copia de seguridad en: " + copia);
            MigracionLegacy.ejecutar(cn);
        } else {
            crearEsquema(cn);
        }
        asegurarColumnas(cn);
        validarEsquema(cn, path);
        sembrarUsuarioInicial(cn);
    }

    static void crearEsquema(Connection cn) throws SQLException {
        String sql;
        try (InputStream in = ConexionBD.class.getResourceAsStream(RECURSO_ESQUEMA)) {
            if (in == null) throw new SQLException("No se encontró el recurso " + RECURSO_ESQUEMA);
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer schema.sql: " + e.getMessage(), e);
        }
        StringBuilder limpio = new StringBuilder();
        for (String linea : sql.split("\\R")) if (!linea.stripLeading().startsWith("--")) limpio.append(linea).append('\n');
        try (Statement st = cn.createStatement()) {
            for (String sentencia : limpio.toString().split(";\\s*\\n")) {
                if (!sentencia.isBlank()) st.executeUpdate(sentencia);
            }
        }
    }

    /** Bases creadas por otras versiones del proyecto a las que solo les falta una columna. */
    private static void asegurarColumnas(Connection cn) throws SQLException {
        if (tablaExiste(cn, "cliente") && !columnas(cn, "cliente").contains("telefono")) {
            try (Statement st = cn.createStatement()) {
                st.executeUpdate("ALTER TABLE cliente ADD COLUMN telefono TEXT NOT NULL DEFAULT ''");
            }
        }
    }

    private static void validarEsquema(Connection cn, Path path) throws SQLException {
        Map<String, List<String>> requeridas = new LinkedHashMap<>();
        requeridas.put("usuario", List.of("idusuario", "user", "password", "nombre", "apellidos", "tipoUser"));
        requeridas.put("producto", List.of("idproducto", "nombre", "precio", "stock"));
        requeridas.put("cliente", List.of("idcliente", "nombre", "dni", "telefono"));
        requeridas.put("venta", List.of("idventa", "fecha", "idcliente", "total", "monto_pagado", "saldo_pendiente", "tipo_pago"));
        requeridas.put("detalle_venta", List.of("idventa", "idproducto", "cantidad", "precio_unitario", "subtotal"));
        requeridas.put("deudor", List.of("idcliente", "deuda"));
        requeridas.put("deudor_ingreso", List.of("idcliente", "fecha", "detalle", "monto"));
        requeridas.put("cobro_deuda", List.of("idcliente", "fecha", "monto"));
        requeridas.put("egreso", List.of("fecha", "descripcion", "monto"));
        for (Map.Entry<String, List<String>> e : requeridas.entrySet()) {
            List<String> actuales = columnas(cn, e.getKey());
            for (String col : e.getValue()) {
                if (!actuales.contains(col)) {
                    throw new SQLException("La base de datos " + path + " tiene un esquema distinto al que usa esta versión "
                            + "(falta " + e.getKey() + "." + col + "). Usa otra ruta con -Dapp.db.path=... o renombra ese archivo "
                            + "para que se cree uno nuevo.");
                }
            }
        }
    }

    private static void sembrarUsuarioInicial(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuario")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO usuario(user,password,nombre,apellidos,tipoUser) VALUES(?,?,?,?,?)")) {
            ps.setString(1, USUARIO_INICIAL);
            ps.setString(2, Passwords.hash(CLAVE_INICIAL));
            ps.setString(3, "Administrador");
            ps.setString(4, "");
            ps.setString(5, "Administrador");
            ps.executeUpdate();
        }
        System.out.println("Usuario inicial creado -> usuario: " + USUARIO_INICIAL + " | contraseña: " + CLAVE_INICIAL
                + "  (cámbiala cuanto antes)");
    }

    // ------------------------------------------------------------------ utilidades de esquema

    static boolean tablaExiste(Connection cn, String tabla) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    static List<String> columnas(Connection cn, String tabla) throws SQLException {
        List<String> cols = new ArrayList<>();
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) cols.add(rs.getString("name"));
        }
        return cols;
    }
}
