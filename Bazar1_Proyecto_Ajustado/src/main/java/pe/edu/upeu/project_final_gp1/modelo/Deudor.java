package pe.edu.upeu.project_final_gp1.modelo;

import javafx.beans.property.*;

public class Deudor {
    private final IntegerProperty idDeudor = new SimpleIntegerProperty();
    private final StringProperty nombre = new SimpleStringProperty("");
    private final IntegerProperty dni = new SimpleIntegerProperty();
    private final StringProperty telefono = new SimpleStringProperty("");
    private final DoubleProperty deuda = new SimpleDoubleProperty();

    public Deudor() {}

    /** Constructor compatible con las pruebas y llamadas del proyecto original. */
    public Deudor(int id, String nombre, int dni, double deuda) {
        this(id, nombre, dni, "", deuda);
    }

    public Deudor(int idDeudor, String nombre, int dni, String telefono, double deuda) {
        setIdDeudor(idDeudor);
        setNombre(nombre);
        setDni(dni);
        setTelefono(telefono);
        setDeuda(deuda);
    }

    public int getIdDeudor() { return idDeudor.get(); }
    /** Alias conservado para compatibilidad con el código original. */
    public int getIdCliente() { return getIdDeudor(); }
    public void setIdCliente(int value) { setIdDeudor(value); }
    public void setIdDeudor(int value) { idDeudor.set(value); }
    public IntegerProperty idDeudorProperty() { return idDeudor; }

    public String getNombre() { return nombre.get(); }
    public void setNombre(String value) { nombre.set(value == null ? "" : value.trim()); }
    public StringProperty nombreProperty() { return nombre; }

    public int getDni() { return dni.get(); }
    public void setDni(int value) { dni.set(value); }
    public IntegerProperty dniProperty() { return dni; }

    public String getTelefono() { return telefono.get(); }
    public void setTelefono(String value) { telefono.set(value == null ? "" : value.trim()); }
    public StringProperty telefonoProperty() { return telefono; }

    public double getDeuda() { return deuda.get(); }
    public void setDeuda(double value) { deuda.set(value); }
    public DoubleProperty deudaProperty() { return deuda; }
}
