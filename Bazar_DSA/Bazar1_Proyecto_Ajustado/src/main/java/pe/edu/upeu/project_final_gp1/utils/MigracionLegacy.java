package pe.edu.upeu.project_final_gp1.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Migra una base del esquema antiguo (DNI como texto, cliente/deudor/venta con nombre y DNI repetidos)
 * al esquema actual. Se ejecuta una sola vez y siempre después de hacer copia de seguridad del archivo.
 *
 * <p>Los DNI antiguos que no sean numéricos se convierten a sus dígitos (p. ej. "1n" -> 1); si no tienen
 * ningún dígito, el registro no se puede migrar y se informa por consola (sigue en la copia de seguridad).</p>
 */
final class MigracionLegacy {
    private static final String[] TABLAS = {"cobro_deuda", "deudor_ingreso", "deudor", "detalle_venta", "venta", "cliente", "producto"};

    private MigracionLegacy() {}

    /** Esquema antiguo = existe la tabla cliente pero sin la columna idcliente. */
    static boolean necesita(Connection cn) throws SQLException {
        return ConexionBD.tablaExiste(cn, "cliente") && !ConexionBD.columnas(cn, "cliente").contains("idcliente");
    }

    static void ejecutar(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement()) { st.execute("PRAGMA foreign_keys = OFF"); }
        cn.setAutoCommit(false);
        try {
            for (String t : TABLAS) {
                if (ConexionBD.tablaExiste(cn, t)) {
                    try (Statement st = cn.createStatement()) { st.executeUpdate("ALTER TABLE " + t + " RENAME TO " + t + "_old"); }
                }
            }
            ConexionBD.crearEsquema(cn);
            copiar(cn);
            for (String t : TABLAS) {
                if (ConexionBD.tablaExiste(cn, t + "_old")) {
                    try (Statement st = cn.createStatement()) { st.executeUpdate("DROP TABLE " + t + "_old"); }
                }
            }
            ConexionBD.crearEsquema(cn); // crea los índices que quedaron sin crear por el nombre repetido
            try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA foreign_key_check")) {
                if (rs.next()) throw new SQLException("La migración dejó referencias inválidas en la tabla " + rs.getString(1) + ".");
            }
            cn.commit();
            System.out.println("Migración al esquema nuevo completada.");
        } catch (SQLException | RuntimeException e) {
            try { cn.rollback(); } catch (SQLException ignored) { }
            throw e;
        } finally {
            cn.setAutoCommit(true);
            try (Statement st = cn.createStatement()) { st.execute("PRAGMA foreign_keys = ON"); } catch (SQLException ignored) { }
        }
    }

    private static void copiar(Connection cn) throws SQLException {
        Map<String, Integer> productos = new HashMap<>();   // id antiguo (minúsculas) -> id nuevo
        Map<Integer, Integer> clientes = new HashMap<>();   // DNI -> idcliente

        // ---- productos: los ids numéricos se conservan; los de texto reciben un id nuevo
        List<Object[]> filas = leer(cn, "SELECT idproducto,nombre,precio,stock FROM producto_old");
        for (Object[] f : filas) {
            String viejo = String.valueOf(f[0]).trim();
            if (viejo.matches("\\d{1,9}") && !productos.containsValue(Integer.parseInt(viejo))) {
                int id = Integer.parseInt(viejo);
                try (PreparedStatement ps = cn.prepareStatement("INSERT INTO producto(idproducto,nombre,precio,stock) VALUES(?,?,?,?)")) {
                    ps.setInt(1, id); ps.setString(2, (String) f[1]); ps.setDouble(3, ((Number) f[2]).doubleValue()); ps.setInt(4, ((Number) f[3]).intValue());
                    ps.executeUpdate();
                }
                productos.put(viejo.toLowerCase(), id);
            }
        }
        for (Object[] f : filas) {
            String viejo = String.valueOf(f[0]).trim().toLowerCase();
            if (!productos.containsKey(viejo)) productos.put(viejo, insertarProducto(cn, (String) f[1], ((Number) f[2]).doubleValue(), ((Number) f[3]).intValue()));
        }

        // ---- clientes (del directorio, de las ventas y de los deudores)
        for (Object[] f : leer(cn, "SELECT dni,nombre,telefono FROM cliente_old")) asegurarCliente(cn, clientes, s(f[0]), s(f[1]), s(f[2]));
        for (Object[] f : leer(cn, "SELECT DISTINCT cliente_dni,cliente_nombre,cliente_telefono FROM venta_old")) asegurarCliente(cn, clientes, s(f[0]), s(f[1]), s(f[2]));
        for (Object[] f : leer(cn, "SELECT dni,nombre,telefono FROM deudor_old")) asegurarCliente(cn, clientes, s(f[0]), s(f[1]), s(f[2]));

        // ---- ventas y detalle
        Map<Long, Boolean> ventasOk = new HashMap<>();
        for (Object[] f : leer(cn, "SELECT idventa,fecha,cliente_dni,total,monto_pagado,saldo_pendiente,tipo_pago FROM venta_old")) {
            Integer dni = dniDesde(s(f[2]));
            Integer idCliente = dni == null ? null : clientes.get(dni);
            long idVenta = ((Number) f[0]).longValue();
            if (idCliente == null) { System.err.println("Migración: venta " + idVenta + " omitida (DNI '" + f[2] + "' no válido)."); continue; }
            double saldo = ((Number) f[5]).doubleValue();
            try (PreparedStatement ps = cn.prepareStatement("INSERT INTO venta(idventa,fecha,idcliente,total,monto_pagado,saldo_pendiente,tipo_pago) VALUES(?,?,?,?,?,?,?)")) {
                ps.setLong(1, idVenta); ps.setString(2, s(f[1])); ps.setInt(3, idCliente); ps.setDouble(4, ((Number) f[3]).doubleValue());
                ps.setDouble(5, ((Number) f[4]).doubleValue()); ps.setDouble(6, saldo); ps.setString(7, saldo > 0.005 ? "Crédito" : "Contado");
                ps.executeUpdate();
            }
            ventasOk.put(idVenta, true);
        }
        for (Object[] f : leer(cn, "SELECT idventa,idproducto,nombre_producto,cantidad,precio_unitario,subtotal FROM detalle_venta_old")) {
            if (!ventasOk.containsKey(((Number) f[0]).longValue())) continue;
            String viejo = s(f[1]).trim().toLowerCase();
            Integer idProd = productos.get(viejo);
            if (idProd == null) { // el producto ya no existe en el inventario: se conserva como producto "eliminado" para no perder el historial
                idProd = insertarProducto(cn, "[Eliminado] " + s(f[2]), ((Number) f[4]).doubleValue(), 0);
                productos.put(viejo, idProd);
            }
            try (PreparedStatement ps = cn.prepareStatement("INSERT INTO detalle_venta(idventa,idproducto,cantidad,precio_unitario,subtotal) VALUES(?,?,?,?,?)")) {
                ps.setLong(1, ((Number) f[0]).longValue()); ps.setInt(2, idProd); ps.setInt(3, ((Number) f[3]).intValue());
                ps.setDouble(4, ((Number) f[4]).doubleValue()); ps.setDouble(5, ((Number) f[5]).doubleValue());
                ps.executeUpdate();
            }
        }

        // ---- deudores, cobros e ingresos manuales
        for (Object[] f : leer(cn, "SELECT dni,deuda FROM deudor_old")) {
            Integer id = idPorDni(clientes, s(f[0]));
            if (id == null) { System.err.println("Migración: deudor con DNI '" + f[0] + "' omitido."); continue; }
            upsertDeuda(cn, id, ((Number) f[1]).doubleValue());
        }
        for (Object[] f : leer(cn, "SELECT dni_cliente,fecha,monto FROM cobro_deuda_old")) {
            Integer id = idPorDni(clientes, s(f[0]));
            if (id == null) { System.err.println("Migración: cobro con DNI '" + f[0] + "' omitido."); continue; }
            try (PreparedStatement ps = cn.prepareStatement("INSERT INTO cobro_deuda(idcliente,fecha,monto) VALUES(?,?,?)")) {
                ps.setInt(1, id); ps.setString(2, s(f[1])); ps.setDouble(3, ((Number) f[2]).doubleValue()); ps.executeUpdate();
            }
        }
        if (ConexionBD.tablaExiste(cn, "deudor_ingreso_old")) {
            for (Object[] f : leer(cn, "SELECT dni,fecha,detalle,monto FROM deudor_ingreso_old")) {
                Integer id = idPorDni(clientes, s(f[0]));
                if (id == null) continue;
                try (PreparedStatement ps = cn.prepareStatement("INSERT INTO deudor_ingreso(idcliente,fecha,detalle,monto) VALUES(?,?,?,?)")) {
                    ps.setInt(1, id); ps.setString(2, s(f[1])); ps.setString(3, s(f[2])); ps.setDouble(4, ((Number) f[3]).doubleValue()); ps.executeUpdate();
                }
            }
        }
        // En el esquema antiguo el deudor se borraba al pagar: se recrea con saldo 0 ("Deuda Pagada") para conservar su historial.
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("INSERT OR IGNORE INTO deudor(idcliente,deuda) SELECT DISTINCT idcliente,0 FROM cobro_deuda");
            st.executeUpdate("INSERT OR IGNORE INTO deudor(idcliente,deuda) SELECT DISTINCT idcliente,0 FROM venta WHERE saldo_pendiente>0.005");
        }
        conciliar(cn);
    }

    /** Deja ingresos - pagos = saldo guardado, con un ajuste explícito si el historial antiguo no alcanzaba. */
    private static void conciliar(Connection cn) throws SQLException {
        String sql = "SELECT d.idcliente, d.deuda - (" +
                " COALESCE((SELECT SUM(saldo_pendiente) FROM venta v WHERE v.idcliente=d.idcliente AND v.saldo_pendiente>0.005),0)" +
                " + COALESCE((SELECT SUM(monto) FROM deudor_ingreso i WHERE i.idcliente=d.idcliente),0)" +
                " - COALESCE((SELECT SUM(monto) FROM cobro_deuda c WHERE c.idcliente=d.idcliente),0)) FROM deudor d";
        for (Object[] f : leer(cn, sql)) {
            double dif = Math.round(((Number) f[1]).doubleValue() * 100.0) / 100.0;
            if (Math.abs(dif) < 0.005) continue;
            try (PreparedStatement ps = cn.prepareStatement("INSERT INTO deudor_ingreso(idcliente,fecha,detalle,monto) VALUES(?,?,?,?)")) {
                ps.setInt(1, ((Number) f[0]).intValue()); ps.setString(2, LocalDate.now().toString());
                ps.setString(3, dif > 0 ? "Saldo anterior (registro previo al historial)" : "Ajuste de saldo anterior");
                ps.setDouble(4, dif); ps.executeUpdate();
            }
        }
    }

    // ------------------------------------------------------------------ ayudas

    private static int insertarProducto(Connection cn, String nombre, double precio, int stock) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO producto(nombre,precio,stock) VALUES(?,?,?)")) {
            ps.setString(1, nombre); ps.setDouble(2, precio); ps.setInt(3, stock); ps.executeUpdate();
        }
        return ultimoId(cn);
    }

    private static void asegurarCliente(Connection cn, Map<Integer, Integer> clientes, String dniTexto, String nombre, String telefono) throws SQLException {
        Integer dni = dniDesde(dniTexto);
        if (dni == null) { System.err.println("Migración: cliente '" + nombre + "' omitido (DNI '" + dniTexto + "' no válido)."); return; }
        if (clientes.containsKey(dni)) return;
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO cliente(nombre,dni,telefono) VALUES(?,?,?)")) {
            ps.setString(1, nombre.isBlank() ? "Cliente " + dni : nombre.trim()); ps.setInt(2, dni); ps.setString(3, telefono.trim()); ps.executeUpdate();
        }
        clientes.put(dni, ultimoId(cn));
    }

    private static Integer idPorDni(Map<Integer, Integer> clientes, String dniTexto) {
        Integer dni = dniDesde(dniTexto);
        return dni == null ? null : clientes.get(dni);
    }

    private static void upsertDeuda(Connection cn, int idCliente, double deuda) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO deudor(idcliente,deuda) VALUES(?,?) ON CONFLICT(idcliente) DO UPDATE SET deuda=deuda+excluded.deuda")) {
            ps.setInt(1, idCliente); ps.setDouble(2, deuda); ps.executeUpdate();
        }
    }

    /** "1n" -> 1, "07654321" -> 7654321, "abc" -> null. */
    static Integer dniDesde(String texto) {
        String digitos = texto == null ? "" : texto.replaceAll("\\D", "");
        if (digitos.isEmpty() || digitos.length() > Dni.LONGITUD) return null;
        int valor = Integer.parseInt(digitos);
        return valor > 0 ? valor : null;
    }

    private static int ultimoId(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) { rs.next(); return rs.getInt(1); }
    }

    private static String s(Object o) { return o == null ? "" : o.toString(); }

    private static List<Object[]> leer(Connection cn, String sql) throws SQLException {
        List<Object[]> filas = new ArrayList<>();
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) { Object[] f = new Object[n]; for (int i = 0; i < n; i++) f[i] = rs.getObject(i + 1); filas.add(f); }
        }
        return filas;
    }
}
