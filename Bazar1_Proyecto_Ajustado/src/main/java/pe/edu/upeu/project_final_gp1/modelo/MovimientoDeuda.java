package pe.edu.upeu.project_final_gp1.modelo;

/**
 * Movimiento financiero de un cliente deudor.
 * Se usa tanto para los ingresos de deuda (fecha, detalle de productos y monto)
 * como para los pagos o abonos (fecha, "Abono" y monto).
 */
public record MovimientoDeuda(String fecha, String detalle, double monto) {}
