package com.example.securehiring.service;

import com.example.securehiring.domain.Envelope;
import com.example.securehiring.domain.Member;
import com.example.securehiring.domain.dto.EncryptedEnvelope;
import com.example.securehiring.domain.dto.SignedPayload;
import com.example.securehiring.domain.enums.EnvelopeType;
import com.example.securehiring.domain.enums.Role;
import com.example.securehiring.exception.*;
import com.example.securehiring.repository.EnvelopeRepository;
import com.example.securehiring.repository.MemberRepository;
import com.example.securehiring.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final MemberRepository memberRepository;
    private final EnvelopeRepository envelopeRepository;

    public void createResult(Long envelopeId, String hrName, boolean result) {
        // 1. 채용담당자 확인
        Member hr = memberRepository.findByName(hrName)
                .orElseThrow(() -> new MemberNotFoundException("해당되는 채용담당자를 찾을 수 없습니다."));

        if (!Role.HR.equals(hr.getRole())) {
            throw new IllegalStateException("입력된 이름의 멤버는 채용 담당자 권한이 없습니다.");
        }

        // 2. 이력서 전자봉투 가져오기
        Envelope envelope = envelopeRepository.findById(envelopeId)
                .orElseThrow(() -> new EnvelopeNotFoundException("해당되는 전자봉투를 찾을 수 없습니다."));

        // 3. 키 로드
        Key secretKey = null;
        try {
            secretKey = SymmetricKeyUtil.generateSecretKey();
        } catch (NoSuchAlgorithmException e) {
            throw new KeyProcessingException("키 생성 중 오류가 발생했습니다.");
        }

        if (hr.getCompany().getPublicKey() == null) {
            throw new KeyProcessingException("공개키가 존재하지 않습니다.");
        }
        if (hr.getCompany().getPrivateKey() == null) {
            throw new KeyProcessingException("개인키가 존재하지 않습니다.");
        }

        PublicKey publicKey = null;
        PrivateKey privateKey = null;

        try {
            publicKey = AsymmetricKeyUtil.loadPublicKey(hr.getCompany().getPublicKey());
            privateKey = AsymmetricKeyUtil.loadPrivateKey(hr.getCompany().getPrivateKey());
        } catch (ClassNotFoundException | IOException e) {
            throw new KeyProcessingException("키 로드 중 오류가 발생했습니다.", e);
        }

        // 4. 해시 및 전자서명 생성
        String resultContent = result ? "합격" : "불합격";
        byte[] resultBytes = resultContent.getBytes();
        byte[] hash = null;
        byte[] signature = null;
        try {
            hash = HashUtil.calcHashVal(resultBytes);
            signature = SignatureUtil.signData(privateKey, hash);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            Arrays.fill(resultBytes, (byte) 0);
            throw new CryptoException("채용결과 해시값 또는 전자서명 생성 중 오류가 발생했습니다.", e);
        } finally {
            if (hash != null) {
                Arrays.fill(hash, (byte) 0);
            }
        }

        // 5. SignedPayload 직렬화 및 암호화(암호문 생성)
        SignedPayload payload = new SignedPayload(resultBytes, signature, publicKey);

        byte[] signedPayloadBytes = null;
        byte[] encryptedSignedPayload = null;
        try {
            signedPayloadBytes = SignedPayload.serializeToBytes(payload);
            encryptedSignedPayload = CipherUtil.encrypt(signedPayloadBytes, secretKey);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException e){
            throw new CryptoException("암호문 생성 중 오류가 발생했습니다.", e);
        } finally {
            Arrays.fill(resultBytes, (byte) 0);
            Arrays.fill(signature, (byte) 0);

            if (signedPayloadBytes != null) {
                Arrays.fill(signedPayloadBytes, (byte) 0);
            }
        }

        // 6. 지원자 공개키로 비밀키 암호화 (전자봉투 생성)
        if (envelope.getApplicant().getPublicKey() == null) {
            throw new KeyProcessingException("공개키가 존재하지 않습니다.");
        }

        byte[] encryptedSecretKey = null;
        try {
            PublicKey applicantPublicKey = AsymmetricKeyUtil.loadPublicKey(envelope.getApplicant().getPublicKey());
            encryptedSecretKey = CipherUtil.encrypt(secretKey.getEncoded(), applicantPublicKey);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | IOException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException | ClassNotFoundException e) {
            throw new CryptoException("전자봉투 생성 중 오류가 발생했습니다", e);
        }

        // 7. Envelope 직렬화 및 저장
        EncryptedEnvelope envelopeDto = new EncryptedEnvelope(encryptedSignedPayload, encryptedSecretKey);
        byte[] envelopeBytes = EncryptedEnvelope.serializeToBytes(envelopeDto);

        Envelope resultEnvelope = Envelope.builder()
                .envelopeData(envelopeBytes)
                .envelopeType(EnvelopeType.RESULT)
                .applicant(envelope.getApplicant())
                .company(hr.getCompany())
                .build();

        envelopeRepository.save(resultEnvelope);

        Arrays.fill(encryptedSignedPayload, (byte) 0);
        Arrays.fill(encryptedSecretKey, (byte) 0);
        Arrays.fill(envelopeBytes, (byte) 0);
    }

    public List<Envelope> getReceivedResults(String applicantName){
        //지원자 확인
        Member applicant = memberRepository.findByName(applicantName)
                .orElseThrow(() -> new MemberNotFoundException("해당되는 지원자를 찾을 수 없습니다."));

        //getResumes가 null이면 빈 리스트 반환
        return applicant.getEnvelopes().stream()
                .filter(envelope -> envelope.getEnvelopeType() == EnvelopeType.RESULT)
                .toList();
    }

    public byte[] verifyResult(Long envelopeId, String applicantName) {
        //1. 채용 담당자 및 지원자 확인
        Envelope envelope = envelopeRepository.findById(envelopeId)
                .orElseThrow(() -> new EnvelopeNotFoundException("해당되는 전자봉투를 찾을 수 없습니다."));

        Member applicant = memberRepository.findByName(applicantName)
                .orElseThrow(() -> new MemberNotFoundException("해당되는 지원자를 찾을 수 없습니다."));

        //2. 지원자의 개인키 가져오기
        if (applicant.getPrivateKey() == null) {
            throw new IllegalStateException("개인키가 존재하지 않습니다.");
        }

        PrivateKey applicantPrivateKey = null;
        try {
            applicantPrivateKey = AsymmetricKeyUtil.loadPrivateKey(applicant.getPrivateKey());
        } catch (IOException | ClassNotFoundException e) {
            throw new KeyProcessingException("지원자의 개인키를 불러오는 중 문제가 발생했습니다.", e);
        }

        //3. 전자봉투 역직렬화
        byte[] envelopeBytes = envelope.getEnvelopeData();
        EncryptedEnvelope decryptedEnvelope = EncryptedEnvelope.deserializeFromBytes(envelopeBytes);
        Arrays.fill(envelopeBytes, (byte) 0);

        //4. 비밀키 복호화
        if (decryptedEnvelope.getEncryptedSecretKey() == null) {
            throw new KeyProcessingException("비밀키가 존재하지 않습니다.");
        }

        Key secretKey = null;
        byte[] secretKeyBytes = null;
        try {
            secretKeyBytes = CipherUtil.decrypt(decryptedEnvelope.getEncryptedSecretKey(), applicantPrivateKey);
            secretKey = new SecretKeySpec(secretKeyBytes, SymmetricKeyUtil.getAlgorithm()); //byte[] -> Key로 변환
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException e) {
            throw new KeyProcessingException("비밀키 복호화 중 오류가 발생했습니다.", e);
        } finally {
            if (secretKeyBytes != null) {
                Arrays.fill(secretKeyBytes, (byte) 0);
            }
        }

        //5. payload 복호화 및 SignedPayload 역직렬화
        byte[] payloadBytes = null;
        try {
            payloadBytes = CipherUtil.decrypt(decryptedEnvelope.getEncryptedSignedPayload(), secretKey);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException e) {
            throw new CryptoException("암호문 복호화 중 오류가 발생했습니다.", e);
        }
        SignedPayload payload = SignedPayload.deserializeFromBytes(payloadBytes);
        Arrays.fill(payloadBytes, (byte) 0);

        //6. 복호화된 이력서의 해시값 계산 및 서명 검증
        boolean valid = false;
        byte[] calculatedHash = null;
        try {
            calculatedHash = HashUtil.calcHashVal(payload.getContent());
            valid = SignatureUtil.verifyData(payload.getSenderPublicKey(), calculatedHash, payload.getSignature());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new CryptoException("해시값 또는 전자서명 검증 중 오류가 발생했습니다.", e);
        } finally {
            if (calculatedHash != null) {
                Arrays.fill(calculatedHash, (byte) 0);
            }
        }

        if (!valid) {
            return new byte[0];
        }

        byte[] content = payload.getContent();
        byte[] result = Arrays.copyOf(content, content.length);
        Arrays.fill(content, (byte) 0);

        return result;
    }
}
