package pe.edu.upeu.project_final_gp1.utils;

/** Utilidades para consultas SQL. */
public final class Sql {
    private Sql() {}

    /**
     * Prepara un texto para usarlo en LIKE ... ESCAPE '\': los caracteres % y _ que escribe
     * el usuario se tratan como texto normal y ya no funcionan como comodines.
     */
    public static String contiene(String texto) {
        String t = texto == null ? "" : texto.trim();
        return "%" + t.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
