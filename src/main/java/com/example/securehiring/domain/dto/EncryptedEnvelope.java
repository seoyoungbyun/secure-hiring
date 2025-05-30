package com.example.securehiring.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.*;

@Getter
@NoArgsConstructor
public class EncryptedEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] encryptedSignedPayload;  //암호화 된 서명 데이터
    private byte[] encryptedSecretKey;  //암호화 된 비밀키

    public EncryptedEnvelope(byte[] encryptedSignedPayload, byte[] encryptedSecretKey) {
        super();
        this.encryptedSignedPayload = encryptedSignedPayload;
        this.encryptedSecretKey = encryptedSecretKey;
    }

    // 객체 직렬화: EncryptedEnvelope -> byte[]
    public static byte[] serializeToBytes(EncryptedEnvelope envelope) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(envelope);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("EncryptedEnvelope 직렬화 중 오류가 발생했습니다.", e);   //exception 새로 정의
        }
    }

    // 역직렬화: byte[] -> EncryptedEnvelope
    public static EncryptedEnvelope deserializeFromBytes(byte[] dataBytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(dataBytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            Object obj = ois.readObject();
            return (EncryptedEnvelope) obj;

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("EncryptedEnvelope 역직렬화 중 오류가 발생했습니다.", e);  //exception 새로 정의
        }
    }
}
