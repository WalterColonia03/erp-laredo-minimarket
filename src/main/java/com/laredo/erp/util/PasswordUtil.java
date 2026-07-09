package com.laredo.erp.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Cumple NFR-007: hash con salt, costo >= 10, nunca MD5/SHA-1/SHA-256 sin sal.
 * BCrypt ya incluye el salt dentro del propio hash generado — no hace
 * falta guardar una columna de salt aparte.
 */
public class PasswordUtil {

    private static final int COSTO = 12;

    public static String hashear(String passwordPlano) {
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(COSTO));
    }

    public static boolean verificar(String passwordPlano, String hashGuardado) {
        return BCrypt.checkpw(passwordPlano, hashGuardado);
    }
}
