package pe.edu.upeu.project_final_gp1.modelo;

public class Cliente {
    private int idCliente;
    private String nombre;
    private int dni;

    public Cliente() {
    }

    public Cliente(int idCliente, String nombre, int dni) {
        this.idCliente = idCliente;
        this.nombre = nombre == null ? "" : nombre.trim();
        this.dni = dni;
    }

    public Cliente(String nombre, int dni) {
        this(0, nombre, dni);
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int value) {
        idCliente = value;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String value) {
        nombre = value == null ? "" : value.trim();
    }

    public int getDni() {
        return dni;
    }

    public void setDni(int value) {
        dni = value;
    }

    @Override
    public String toString() {
        return nombre + (dni == 0 ? "" : " · " + dni);
    }
}
