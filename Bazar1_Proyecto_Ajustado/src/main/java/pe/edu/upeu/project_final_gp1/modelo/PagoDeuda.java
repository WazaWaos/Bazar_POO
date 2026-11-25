package pe.edu.upeu.project_final_gp1.modelo;

import javafx.beans.property.*;

public class PagoDeuda {
    private final IntegerProperty idCobro = new SimpleIntegerProperty();
    private final IntegerProperty idCliente = new SimpleIntegerProperty();
    private final StringProperty nombre = new SimpleStringProperty("");
    private final IntegerProperty dni = new SimpleIntegerProperty();
    private final StringProperty fecha = new SimpleStringProperty("");
    private final DoubleProperty monto = new SimpleDoubleProperty();

    public PagoDeuda(int idCobro, int idCliente, String nombre, int dni, String fecha, double monto) {
        setIdCobro(idCobro);
        setIdCliente(idCliente);
        setNombre(nombre);
        setDni(dni);
        setFecha(fecha);
        setMonto(monto);
    }

    public int getIdCobro() {
        return idCobro.get();
    }

    public void setIdCobro(int value) {
        idCobro.set(value);
    }

    public IntegerProperty idCobroProperty() {
        return idCobro;
    }

    public int getIdCliente() {
        return idCliente.get();
    }

    public void setIdCliente(int value) {
        idCliente.set(value);
    }

    public IntegerProperty idClienteProperty() {
        return idCliente;
    }

    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String value) {
        nombre.set(value == null ? "" : value);
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public int getDni() {
        return dni.get();
    }

    public void setDni(int value) {
        dni.set(value);
    }

    public IntegerProperty dniProperty() {
        return dni;
    }

    public String getFecha() {
        return fecha.get();
    }

    public void setFecha(String value) {
        fecha.set(value == null ? "" : value);
    }

    public StringProperty fechaProperty() {
        return fecha;
    }

    public double getMonto() {
        return monto.get();
    }

    public void setMonto(double value) {
        monto.set(value);
    }

    public DoubleProperty montoProperty() {
        return monto;
    }
}
