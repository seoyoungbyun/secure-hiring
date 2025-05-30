package com.example.securehiring.util;

import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

@Component
public class SymmetricKeyUtil {

    private static final Path keyDir = Paths.get("uploads/keys");

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128;

    public static Key generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        return keyGen.generateKey();
    }

    public static void saveSecretKey(String fname, Key secretKey) throws IOException {
        if (!Files.exists(keyDir)) {
            Files.createDirectories(keyDir); // 디렉토리가 없으면 생성
        }

        try(FileOutputStream fstream = new FileOutputStream(keyDir.toString() + fname);
            ObjectOutputStream ostream = new ObjectOutputStream(fstream)){
            ostream.writeObject(secretKey);
        }
    }

    public static Key loadSecretKey(String fname) throws ClassNotFoundException, IOException {
        try (FileInputStream fis = new FileInputStream(keyDir.toString() + fname);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            return (Key) obj;
        }
    }
}
