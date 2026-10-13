package com.lqx.health_check_system.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CryptoUtils {

    @Value("${crypto.aes.key}")
    private String aesKeyB64;

    @Value("${crypto.aes.iv}")
    private String aesIvB64;

    private static CryptoUtils INSTANCE;

    @PostConstruct
    public void init() {
        INSTANCE = this;
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), buildIv());
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    public static String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) return ciphertext;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), buildIv());
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ciphertext;
        }
    }

    private static SecretKeySpec buildKey() {
        byte[] key = Base64.getDecoder().decode(INSTANCE.aesKeyB64);
        return new SecretKeySpec(key, "AES");
    }

    private static IvParameterSpec buildIv() {
        byte[] iv = Base64.getDecoder().decode(INSTANCE.aesIvB64);
        return new IvParameterSpec(iv);
    }
}