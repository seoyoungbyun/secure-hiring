package com.example.securehiring.service;

import com.example.securehiring.domain.Member;
import com.example.securehiring.domain.dto.ResultResponse;
import com.example.securehiring.domain.dto.SignedPayload;
import com.example.securehiring.exception.CryptoException;
import com.example.securehiring.exception.KeyProcessingException;
import com.example.securehiring.util.*;
import com.example.securehiring.domain.ResultNotification;
import com.example.securehiring.repository.ResultNotificationRepository;
import com.example.securehiring.domain.dto.EncryptedEnvelope;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultViewerService {

    private final ResultNotificationRepository resultNotificationRepository;

    public List<ResultResponse> viewResultsByApplicant(Member applicant) {

        List<ResultNotification> resultList = resultNotificationRepository
                .findAllByApplicant(applicant);

        return resultList.stream().map(result -> {

            try {
                // 1. 지원자의 개인키 가져오기 전에 null 체크
                if (applicant.getPrivateKey() == null) {
                    throw new IllegalArgumentException("개인키가 존재하지 않습니다.");
                }

                PrivateKey applicantPrivateKey;
                try {
                    applicantPrivateKey = AsymmetricKeyUtil.loadPrivateKey(applicant.getPrivateKey());
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new KeyProcessingException("지원자의 개인키를 불러오는 중 문제가 발생했습니다.");
                }

                // 2. 전자봉투 역직렬화
                byte[] envelopeBytes = result.getEnvelope().getEnvelopeData();
                EncryptedEnvelope decryptedEnvelope = EncryptedEnvelope.deserializeFromBytes(envelopeBytes);

                // 3. 비밀키 복호화
                Key secretKey = null;
                try {
                    byte[] secretKeyBytes = CipherUtil.decrypt(decryptedEnvelope.getEncryptedSecretKey(), applicantPrivateKey);
                    secretKey = new SecretKeySpec(secretKeyBytes, "AES");
                    Arrays.fill(secretKeyBytes, (byte) 0); // 비밀키 바이트 클리어
                } catch (RuntimeException | NoSuchPaddingException | IllegalBlockSizeException |
                         NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
                    throw new KeyProcessingException("비밀키 복호화 중 문제가 발생했습니다.");
                }

                // 4. payload 복호화 및 SignedPayload 역직렬화
                byte[] payloadBytes = null;
                try {
                    payloadBytes = CipherUtil.decrypt(decryptedEnvelope.getEncryptedSignedPayload(), secretKey);
                    Arrays.fill(payloadBytes, (byte) 0);
                } catch (RuntimeException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
                    e.printStackTrace();
                    throw new CryptoException("암호문 복호화 중 오류가 발생했습니다.");
                }
                SignedPayload payload = SignedPayload.deserializeFromBytes(payloadBytes);

                // 5. 복호화된 채용 결과 해시값 계산 및 서명 검증
                byte[] content = null;
                boolean valid;
                try {
                    content = payload.getContent();
                    byte[] calculatedHash = HashUtil.calcHashVal(content);
                    valid = SignatureUtil.verifyData(payload.getSenderPublicKey(), calculatedHash, payload.getSignature());
                    Arrays.fill(calculatedHash, (byte) 0);
                } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                    e.printStackTrace();
                    throw new CryptoException("해시값 또는 전자서명 검증 중 오류가 발생했습니다.");
                }

                if (!valid) {
                    throw new SecurityException("전자서명 검증을 실패했습니다. 데이터 위조 가능성이 있습니다.");
                }

                String resultMessage = new String(payload.getContent());
                Arrays.fill(content, (byte) 0);
                return new ResultResponse(resultMessage);

            } catch (RuntimeException e) {
                throw new RuntimeException("결과 복호화 실패: " + e.getMessage());
            }
        }).collect(Collectors.toList());
    }
}
