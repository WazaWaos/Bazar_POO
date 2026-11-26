package pe.edu.upeu.project_final_gp1.utils;

import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

/**
 * Reglas del DNI peruano (8 dígitos). Se guarda como entero, pero SIEMPRE se muestra
 * con ceros a la izquierda: el DNI 07654321 se guarda como 7654321 y se imprime 07654321.
 */
public final class Dni {
    public static final int LONGITUD = 8;
    public static final int MAXIMO = 99_999_999;

    private Dni() {}

    /** Convierte el texto escrito por el usuario. Exige exactamente 8 dígitos (se aceptan ceros iniciales). */
    public static int parse(String texto) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("El DNI es obligatorio.");
        if (!t.matches("\\d+")) throw new IllegalArgumentException("El DNI solo puede contener números.");
        if (t.length() != LONGITUD) throw new IllegalArgumentException("El DNI debe tener " + LONGITUD + " dígitos.");
        int valor = Integer.parseInt(t);
        validar(valor);
        return valor;
    }

    /** Validación de rango usada también por los DAO. */
    public static void validar(int dni) {
        if (dni <= 0 || dni > MAXIMO) throw new IllegalArgumentException("El DNI debe ser un número de 8 dígitos.");
    }

    public static String format(int dni) {
        return String.format("%0" + LONGITUD + "d", dni);
    }

    /** Filtro para TextField: solo dígitos y máximo 8 caracteres. */
    public static TextFormatter<String> filtro() {
        UnaryOperator<TextFormatter.Change> f = c -> c.getControlNewText().matches("\\d{0," + LONGITUD + "}") ? c : null;
        return new TextFormatter<>(f);
    }
}
