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

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
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
                //1. 지원자의 개인키 가져오기
                PrivateKey applicantPrivateKey = null;
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
                } catch (Exception e) {
                    throw new KeyProcessingException("비밀키 복호화 중 문제가 발생했습니다.");
                }

                // 4. payload 복호화 및 SignedPayload 역직렬화
                byte[] payloadBytes = null;
                try {
                    payloadBytes = CipherUtil.decrypt(decryptedEnvelope.getEncryptedSignedPayload(), secretKey);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new CryptoException("암호문 복호화 중 오류가 발생했습니다.");
                }
                SignedPayload payload = SignedPayload.deserializeFromBytes(payloadBytes);

                // 5. 복호화된 채용 결과 해시값 계산 및 서명 검증
                boolean valid = false;
                try {
                    byte[] calculatedHash = HashUtil.calcHashVal(payload.getContent());
                    valid = SignatureUtil.verifyData(payload.getSenderPublicKey(), calculatedHash, payload.getSignature());
                } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                    e.printStackTrace();
                    throw new CryptoException("해시값 또는 전자서명 검증 중 오류가 발생했습니다.");
                }

                if (!valid) {
                    throw new SecurityException("전자서명 검증을 실패했습니다. 데이터 위조 가능성이 있습니다.");
                }

                String resultMessage = new String(payload.getContent());
                return new ResultResponse(resultMessage);

            } catch (Exception e) {
                throw new RuntimeException("결과 복호화 실패: " + e.getMessage());
            }
        }).collect(Collectors.toList());
    }
}
