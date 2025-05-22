package com.example.securehiring.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.*;
import java.security.PublicKey;

@Getter
@NoArgsConstructor
public class SignedPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] content; // 원문 내용
    private byte[] signature;   // 전자서명
    private PublicKey senderPublicKey;  //보내는 사람의 공개키

    public SignedPayload(byte[] content, byte[] signature, PublicKey senderPublicKey) {
        super();
        this.content = content;
        this.signature = signature;
        this.senderPublicKey = senderPublicKey;
    }

    // 객체 직렬화: SignedPayload -> byte[]
    static byte[] serializeToBytes(SignedPayload payload){
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(payload);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("SignedPayload 직렬화 중 오류가 발생했습니다.", e);   //exception 새로 정의
        }
    }

    // 역직렬화: byte[] -> SignedPayload
    static SignedPayload deserializeFromBytes(byte[] dataBytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(dataBytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            Object obj = ois.readObject();
            return (SignedPayload) obj;

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("SignedPayload 역직렬화 중 오류가 발생했습니다.", e);  //exception 새로 정의
        }
    }
}
