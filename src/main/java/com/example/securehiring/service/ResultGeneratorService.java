package com.example.securehiring.service;

import com.example.securehiring.domain.Envelope;
import com.example.securehiring.domain.dto.EncryptedEnvelope;
import com.example.securehiring.domain.dto.ResultRequest;
import com.example.securehiring.domain.enums.EnvelopeType;
import com.example.securehiring.exception.CryptoException;
import com.example.securehiring.exception.KeyProcessingException;
import com.example.securehiring.repository.EnvelopeRepository;
import com.example.securehiring.util.*;
import com.example.securehiring.domain.ResultNotification;
import com.example.securehiring.domain.dto.SignedPayload;
import com.example.securehiring.repository.ResultNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.*;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ResultGeneratorService {

    private final ResultNotificationRepository resultNotificationRepository;
    private final EnvelopeRepository envelopeRepository;

    public void createResult(Long envelopeId, ResultRequest request) {
        try {
            Key appliSecretKey = SymmetricKeyUtil.generateSecretKey();
            SymmetricKeyUtil.saveSecretKey("applicant1_secret.key", appliSecretKey);

            KeyPair companyKeyPair = AsymmetricKeyUtil.generateKeyPair();
            AsymmetricKeyUtil.savePrivateKey("company1_private.key", companyKeyPair.getPrivate());
            AsymmetricKeyUtil.savePublicKey("company1_public.key", companyKeyPair.getPublic());

            KeyPair applicantKeyPair = AsymmetricKeyUtil.generateKeyPair();
            AsymmetricKeyUtil.savePrivateKey("applicant1_private.key", applicantKeyPair.getPrivate());
            AsymmetricKeyUtil.savePublicKey("applicant1_public.key", applicantKeyPair.getPublic());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }

        // 1. Envelope 조회
        Envelope resumeEnvelope;
        try {
            resumeEnvelope = envelopeRepository.findById(envelopeId).orElseThrow();
        } catch (NoSuchElementException e) {
            throw new RuntimeException("Envelope not found", e);
        }

        // 2. 결과 문자열 구성 (예: "합격", "불합격")
        String resultContent = request.getResult() ? "합격" : "불합격";

        // 3. 키 로드
        Key secretKey = null;
        PublicKey companyPublicKey  = null;
        PrivateKey companyPrivateKey = null;
        try {
            secretKey = SymmetricKeyUtil.loadSecretKey(
                    resumeEnvelope.getSender().getSecretKey());
            companyPublicKey  = AsymmetricKeyUtil.loadPublicKey(
                    resumeEnvelope.getTargetCompany().getPublicKey());
            companyPrivateKey = AsymmetricKeyUtil.loadPrivateKey(
                    resumeEnvelope.getTargetCompany().getPrivateKey());
        } catch (ClassNotFoundException | IOException e){
            e.printStackTrace();
            throw new KeyProcessingException("지원자의 키를 불러오는 중 오류가 발생했습니다.");
        }

        // 4. 해시 및 전자서명 생성
        byte[] resultBytes = null;
        byte[] signature = null;
        try {
            resultBytes = resultContent.getBytes();
            byte[] hash = HashUtil.calcHashVal(resultBytes);
            signature = SignatureUtil.signData(companyPrivateKey, hash);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            throw new CryptoException("채용 결과 해시값 또는 전자서명 생성 중 오류가 발생했습니다.");
        }

        // 5. SignedPayload 생성 및 직렬화
        SignedPayload payload  = new SignedPayload(resultBytes, signature, companyPublicKey);
        byte[] signedPayloadBytes = SignedPayload.serializeToBytes(payload);

        // 6. 비밀키로 payload 암호화(암호문 생성)
        byte[] encryptedSignedPayload = null;
        try {
            encryptedSignedPayload = CipherUtil.encrypt(signedPayloadBytes, secretKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CryptoException("암호문 생성 중 오류가 발생했습니다.");
        }

        // 7. 지원자 공개키로 비밀키 암호화 (전자봉투 생성)
        byte[] encryptedSecretKey = null;
        try {
            PublicKey applicantPublicKey = AsymmetricKeyUtil.loadPublicKey(resumeEnvelope.getSender().getPublicKey());
            encryptedSecretKey = CipherUtil.encrypt(secretKey.getEncoded(), applicantPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CryptoException("전자봉투 생성 중 오류가 발생했습니다");
        }

        // 8. Envelope 직렬화 및 저장
        EncryptedEnvelope envelopeDto = new EncryptedEnvelope(encryptedSignedPayload, encryptedSecretKey);
        byte[] envelopeBytes = EncryptedEnvelope.serializeToBytes(envelopeDto);

        Envelope resultEnvelope = Envelope.builder()
                .envelopeData(envelopeBytes)
                .envelopeType(EnvelopeType.RESULT)
                .targetCompany(resumeEnvelope.getTargetCompany())
                .sender(resumeEnvelope.getSender())
                .build();
        envelopeRepository.save(resultEnvelope);

        // 9. ResultNotification 저장
        ResultNotification result = ResultNotification.builder()
                .result(request.getResult())
                .applicant(resumeEnvelope.getSender())
                .company(resumeEnvelope.getTargetCompany())
                .envelope(resultEnvelope)
                .build();
        resultNotificationRepository.save(result);
    }
}
