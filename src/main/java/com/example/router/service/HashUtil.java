package com.example.router.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * hash(pwd, "sha3-512") -> байты хеша, представленные в base64.
 *
 * SHA3-512 доступен "из коробки" в JDK (провайдер SUN) начиная с Java 9,
 * дополнительные библиотеки не нужны.
 */
@Component
public class HashUtil {

    /**
     * Основной метод: SHA3-512(input) -> base64-строка байт хеша.
     */
    public String sha3_512Base64(String input) {
        return Base64.getEncoder().encodeToString(sha3_512Bytes(input));
    }

    /**
     * Оставлено для отладки/сверки — hex-представление того же хеша.
     */
    public String sha3_512Hex(String input) {
        byte[] hash = sha3_512Bytes(input);
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] sha3_512Bytes(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-512");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA3-512 недоступен в текущей JVM", e);
        }
    }
}
