package com.zxyy.order.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private PasswordUtil() {}
    public static String encrypt(String value) { return ENCODER.encode(value); }
    public static boolean matches(String value, String hash) {
        return value != null && hash != null && ENCODER.matches(value, hash);
    }
}
