package com.example.securehiring.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.security.Key;

@Component
public class CipherUtil {

    private static final String ALGORITHM = "AES";

    public static byte[] encrypt(byte[] data, Key secretKey) throws Exception {
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, secretKey);
        return c.doFinal(data);
    }

    public static byte[] decrypt(byte[] encryptedData, Key secretKey) throws Exception {
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, secretKey);
        return c.doFinal(encryptedData);
    }
}
