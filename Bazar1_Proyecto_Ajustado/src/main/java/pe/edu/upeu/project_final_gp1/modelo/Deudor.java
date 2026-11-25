package pe.edu.upeu.project_final_gp1.modelo;

import javafx.beans.property.*;

public class Deudor {
    private final IntegerProperty idCliente = new SimpleIntegerProperty();
    private final StringProperty nombre = new SimpleStringProperty("");
    private final IntegerProperty dni = new SimpleIntegerProperty();
    private final DoubleProperty deuda = new SimpleDoubleProperty();

    public Deudor() {
    }

    public Deudor(int idCliente, String nombre, int dni, double deuda) {
        setIdCliente(idCliente);
        setNombre(nombre);
        setDni(dni);
        setDeuda(deuda);
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

    public double getDeuda() {
        return deuda.get();
    }

    public void setDeuda(double value) {
        deuda.set(value);
    }

    public DoubleProperty deudaProperty() {
        return deuda;
    }
}