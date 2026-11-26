package pe.edu.upeu.project_final_gp1.utils;

/** Lectura estricta de importes escritos por el usuario. */
public final class Dinero {
    private Dinero() {}

    /**
     * Acepta "12", "12.5", "12,50". Rechaza lo que Double.parseDouble aceptaría por error:
     * "12d", "1e3", "NaN", "0x1p3", más de 2 decimales, signos o separadores de miles.
     */
    public static double parse(String texto, String mensajeError) {
        String t = texto == null ? "" : texto.trim().replace(',', '.');
        if (!t.matches("\\d+(\\.\\d{1,2})?")) throw new IllegalArgumentException(mensajeError);
        return Double.parseDouble(t);
    }
}
