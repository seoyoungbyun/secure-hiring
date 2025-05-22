package com.example.securehiring.util;

import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import java.io.*;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

@Component
public class SymmetricKeyUtil {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128;

    public static Key generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        return keyGen.generateKey();
    }

    public static boolean saveSecretKey(String fname, Key secretKey) throws IOException {
        try(FileOutputStream fstream = new FileOutputStream(fname);
            ObjectOutputStream ostream = new ObjectOutputStream(fstream)){
            ostream.writeObject(secretKey);
            return true;
        }
    }

    public static Key loadSecretKey(String fname) throws ClassNotFoundException, IOException {
        try (FileInputStream fis = new FileInputStream(fname);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            return (Key) obj;
        }
    }
}
