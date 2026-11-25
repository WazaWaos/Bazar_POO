package pe.edu.upeu.project_final_gp1.modelo;

public class Usuario {
    private int idusuario;
    private String user;
    private String password;
    private String nombre;
    private String apellidos;
    private String tipoUser;

    public Usuario() {}

    public int getIdusuario() { return idusuario; }
    public void setIdusuario(int value) { idusuario = value; }
    public String getUser() { return user; }
    public void setUser(String value) { user = value; }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getNombre() { return nombre; }
    public void setNombre(String value) { nombre = value; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String value) { apellidos = value; }
    public String getTipoUser() { return tipoUser; }
    public void setTipoUser(String value) { tipoUser = value; }
}
