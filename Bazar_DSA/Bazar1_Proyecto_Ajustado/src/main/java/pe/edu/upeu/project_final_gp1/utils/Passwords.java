package pe.edu.upeu.project_final_gp1.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hash de contraseñas con PBKDF2 (incluido en el JDK, sin dependencias externas). */
public final class Passwords {
    private static final String PREFIJO = "pbkdf2";
    private static final int ITERACIONES = 120_000;
    private static final int BITS = 256;

    private Passwords() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] h = derivar(password.toCharArray(), salt, ITERACIONES);
        Base64.Encoder b = Base64.getEncoder();
        return PREFIJO + "$" + ITERACIONES + "$" + b.encodeToString(salt) + "$" + b.encodeToString(h);
    }

    /** true si el valor guardado ya es un hash (y no una contraseña antigua en texto plano). */
    public static boolean esHash(String almacenado) {
        return almacenado != null && almacenado.startsWith(PREFIJO + "$");
    }

    public static boolean verificar(String password, String almacenado) {
        if (password == null || almacenado == null) return false;
        if (!esHash(almacenado)) return false;
        String[] p = almacenado.split("\\$");
        if (p.length != 4) return false;
        try {
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] esperado = Base64.getDecoder().decode(p[3]);
            byte[] obtenido = derivar(password.toCharArray(), salt, Integer.parseInt(p[1]));
            return MessageDigest.isEqual(esperado, obtenido);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(char[] pass, byte[] salt, int iter) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(pass, salt, iter, BITS)).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña.", e);
        }
    }
}
