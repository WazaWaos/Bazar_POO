package pe.edu.upeu.project_final_gp1.modelo;

public class Cliente {
    private final int dni;
    private final String nombre;
    private final int telefono;

    public Cliente(String nombre, int dni, int telefono) {
        this.nombre = nombre == null ? "" : nombre.trim();
        this.dni = dni;
        this.telefono = telefono;
    }

    public String getNombre() { return nombre; }
    public int getDni() { return dni; }
    public int getTelefono() { return telefono; }

    @Override public String toString() {
        return nombre + (dni > 0 ? " · " + dni : "");
    }
}
