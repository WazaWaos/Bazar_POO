package pe.edu.upeu.project_final_gp1.modelo;

import pe.edu.upeu.project_final_gp1.utils.Dni;

public class Cliente {
    private int idCliente;
    private String nombre = "";
    private int dni;
    private String telefono = "";

    public Cliente() {}

    public Cliente(int idCliente, String nombre, int dni) {
        this(idCliente, nombre, dni, "");
    }

    public Cliente(int idCliente, String nombre, int dni, String telefono) {
        this.idCliente = idCliente;
        this.nombre = nombre == null ? "" : nombre.trim();
        this.dni = dni;
        this.telefono = telefono == null ? "" : telefono.trim();
    }

    public Cliente(String nombre, int dni) {
        this(0, nombre, dni, "");
    }

    public Cliente(String nombre, int dni, String telefono) {
        this(0, nombre, dni, telefono);
    }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int value) { idCliente = value; }
    public String getNombre() { return nombre; }
    public void setNombre(String value) { nombre = value == null ? "" : value.trim(); }
    public int getDni() { return dni; }
    public void setDni(int value) { dni = value; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String value) { telefono = value == null ? "" : value.trim(); }

    @Override
    public String toString() {
        return nombre + (dni == 0 ? "" : " · " + Dni.format(dni));
    }
}
