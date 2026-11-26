package pe.edu.upeu.project_final_gp1.implementacion;

import pe.edu.upeu.project_final_gp1.interfaces.IVentaDao;
import pe.edu.upeu.project_final_gp1.modelo.Cliente;
import pe.edu.upeu.project_final_gp1.modelo.DetalleVenta;
import pe.edu.upeu.project_final_gp1.modelo.Venta;
import pe.edu.upeu.project_final_gp1.utils.ConexionBD;
import pe.edu.upeu.project_final_gp1.utils.Dni;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VentaDaoImpl implements IVentaDao {
    @Override
    public Venta registrarVenta(Venta venta) throws SQLException {
        if (venta == null || venta.getDetalles().isEmpty())
            throw new IllegalArgumentException("Agrega al menos un producto a la venta.");

        if (venta.getNombreCliente().trim().isEmpty())
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        Dni.validar(venta.getDniCliente());

        if (!Double.isFinite(venta.getMontoPagado()))
            throw new IllegalArgumentException("El pago inicial debe ser un importe válido.");

        double total = 0;

        for (DetalleVenta d : venta.getDetalles()) {
            if (d.getIdProducto() <= 0 || d.getCantidad() <= 0 ||
                    !Double.isFinite(d.getPrecioUnitario()) || d.getPrecioUnitario() < 0)
                throw new IllegalArgumentException("El producto, la cantidad y el precio deben ser válidos.");

            total += d.getCantidad() * d.getPrecioUnitario();
        }

        if (!Double.isFinite(total))
            throw new IllegalArgumentException("El total de la venta excede el rango permitido.");

        total = Math.round(total * 100.0) / 100.0;
        double pagado = Math.round(venta.getMontoPagado() * 100.0) / 100.0;

        if (pagado < 0 || pagado - total > 0.005)
            throw new IllegalArgumentException("El pago inicial no puede ser negativo ni superar el total.");

        double saldo = Math.round(Math.max(0, total - pagado) * 100.0) / 100.0;

        venta.setTotal(total);
        venta.setMontoPagado(pagado);
        venta.setSaldoPendiente(saldo);
        venta.setTipoPago(saldo > 0.005 ? "Crédito" : "Contado");

        if (venta.getFecha() == null || venta.getFecha().isBlank())
            venta.setFecha(LocalDate.now().toString());

        try (Connection cn = ConexionBD.getConnection()) {
            cn.setAutoCommit(false);

            try {
                int idCliente = new ClienteDaoImpl().guardarOActualizar(cn,
                        new Cliente(venta.getNombreCliente(), venta.getDniCliente()));

                venta.setIdCliente(idCliente);

                String sqlVenta = "INSERT INTO venta(fecha,idcliente,total,monto_pagado,saldo_pendiente,tipo_pago) VALUES(?,?,?,?,?,?)";

                try (PreparedStatement st = cn.prepareStatement(sqlVenta)) {
                    st.setString(1, venta.getFecha());
                    st.setInt(2, idCliente);
                    st.setDouble(3, total);
                    st.setDouble(4, pagado);
                    st.setDouble(5, saldo);
                    st.setString(6, venta.getTipoPago());
                    st.executeUpdate();
                }

                try (Statement st = cn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) venta.setIdVenta(rs.getInt(1));
                }

                String sqlStock = "UPDATE producto SET stock=stock-? WHERE idproducto=? AND stock>=?";
                String sqlDetalle = "INSERT INTO detalle_venta(idventa,idproducto,cantidad,precio_unitario,subtotal) VALUES(?,?,?,?,?)";

                try (PreparedStatement stock = cn.prepareStatement(sqlStock);
                     PreparedStatement detalle = cn.prepareStatement(sqlDetalle)) {

                    for (DetalleVenta d : venta.getDetalles()) {
                        Double precioActual = precioActual(cn, d.getIdProducto());
                        if (precioActual != null && Math.abs(precioActual - d.getPrecioUnitario()) > 0.005)
                            throw new IllegalArgumentException("El precio de \"" + d.getNombreProducto() + "\" cambió a S/ "
                                    + String.format(java.util.Locale.US, "%.2f", precioActual) + ". Quítalo del carrito y vuelve a agregarlo.");
                        stock.setInt(1, d.getCantidad());
                        stock.setInt(2, d.getIdProducto());
                        stock.setInt(3, d.getCantidad());

                        if (stock.executeUpdate() == 0)
                            throw new SQLException("Stock insuficiente o producto inexistente: " + d.getNombreProducto());

                        detalle.setInt(1, venta.getIdVenta());
                        detalle.setInt(2, d.getIdProducto());
                        detalle.setInt(3, d.getCantidad());
                        detalle.setDouble(4, d.getPrecioUnitario());
                        detalle.setDouble(5, Math.round(d.getCantidad() * d.getPrecioUnitario() * 100.0) / 100.0);
                        detalle.executeUpdate();
                    }
                }

                if (saldo > 0.005) {
                    String upsert = "INSERT INTO deudor(idcliente,deuda) VALUES(?,?) " +
                            "ON CONFLICT(idcliente) DO UPDATE SET deuda=deudor.deuda+excluded.deuda";

                    try (PreparedStatement st = cn.prepareStatement(upsert)) {
                        st.setInt(1, idCliente);
                        st.setDouble(2, saldo);
                        st.executeUpdate();
                    }
                }

                cn.commit();
                return venta;

            } catch (Exception e) {
                try {
                    cn.rollback();
                } catch (SQLException ignored) {
                }

                if (e instanceof SQLException se) throw se;
                if (e instanceof RuntimeException re) throw re;

                throw new SQLException("No se pudo registrar la venta.", e);

            } finally {
                try {
                    cn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /** Precio vigente en inventario (null si el producto no existe: el UPDATE de stock informará el error). */
    private Double precioActual(Connection cn, int idProducto) throws SQLException {
        try (PreparedStatement st = cn.prepareStatement("SELECT precio FROM producto WHERE idproducto=?")) {
            st.setInt(1, idProducto);
            try (ResultSet rs = st.executeQuery()) { return rs.next() ? rs.getDouble(1) : null; }
        }
    }

    @Override
    public List<Venta> listAll() throws SQLException {
        List<Venta> result = new ArrayList<>();

        String sql = "SELECT v.idventa,v.fecha,v.idcliente,c.nombre,c.dni,v.total,v.monto_pagado,v.saldo_pendiente,v.tipo_pago " +
                "FROM venta v INNER JOIN cliente c ON c.idcliente=v.idcliente ORDER BY v.idventa DESC";

        try (Connection cn = ConexionBD.getConnection();
             PreparedStatement st = cn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                Venta v = new Venta();

                v.setIdVenta(rs.getInt("idventa"));
                v.setFecha(rs.getString("fecha"));
                v.setIdCliente(rs.getInt("idcliente"));
                v.setNombreCliente(rs.getString("nombre"));
                v.setDniCliente(rs.getInt("dni"));
                v.setTotal(rs.getDouble("total"));
                v.setMontoPagado(rs.getDouble("monto_pagado"));
                v.setSaldoPendiente(rs.getDouble("saldo_pendiente"));
                v.setTipoPago(rs.getString("tipo_pago"));

                result.add(v);
            }
        }

        return result;
    }
}