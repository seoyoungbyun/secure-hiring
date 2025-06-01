package com.example.securehiring.service;

import com.example.securehiring.domain.Company;
import com.example.securehiring.domain.Envelope;
import com.example.securehiring.domain.Member;
import com.example.securehiring.domain.dto.EncryptedEnvelope;
import com.example.securehiring.domain.dto.SignedPayload;
import com.example.securehiring.domain.enums.EnvelopeType;
import com.example.securehiring.domain.enums.Role;
import com.example.securehiring.exception.*;
import com.example.securehiring.repository.CompanyRepository;
import com.example.securehiring.repository.EnvelopeRepository;
import com.example.securehiring.repository.MemberRepository;
import com.example.securehiring.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final MemberRepository memberRepository;
    private final CompanyRepository companyRepository;
    private final EnvelopeRepository envelopeRepository;

    public void uploadResume(String memberName, String companyName, MultipartFile resumeFile){
        //1. 회원 확인 및 키 생성/로드
        PublicKey publicKey = null;
        PrivateKey privateKey = null;

        Member applicant = memberRepository.findByName(memberName).orElse(null);
        if (applicant == null) {
            try {
                KeyPair keyPair = AsymmetricKeyUtil.generateKeyPair();
                publicKey = keyPair.getPublic();
                privateKey = keyPair.getPrivate();

                String publicKeyPath = memberName + "_public_" + System.currentTimeMillis() + ".key";
                String privateKeyPath = memberName + "_private_" + System.currentTimeMillis() + ".key";

                AsymmetricKeyUtil.savePublicKey(publicKeyPath, publicKey);
                AsymmetricKeyUtil.savePrivateKey(privateKeyPath, privateKey);

                applicant = Member.builder()
                        .name(memberName)
                        .publicKey(publicKeyPath)
                        .privateKey(privateKeyPath)
                        .role(Role.APPLICANT)
                        .build();
                memberRepository.save(applicant);
            } catch (NoSuchAlgorithmException e){
                throw new KeyProcessingException("지원자의 키 생성 중 오류가 발생했습니다.", e);
            } catch (IOException e) {
                throw new KeyProcessingException("지원자의 키 저장 중 오류가 발생했습니다.", e);
            }
        }else{  //기존의 지원자일 경우
            try{
                if (applicant.getPublicKey() == null) {
                    throw new KeyProcessingException("공개키가 존재하지 않습니다.");
                }
                if (applicant.getPrivateKey() == null) {
                    throw new KeyProcessingException("개인키가 존재하지 않습니다.");
                }

                publicKey = AsymmetricKeyUtil.loadPublicKey(applicant.getPublicKey());
                privateKey = AsymmetricKeyUtil.loadPrivateKey(applicant.getPrivateKey());
            }catch (ClassNotFoundException | IOException e){
                throw new KeyProcessingException("지원자의 키를 불러오는 중 오류가 발생했습니다.", e);
            }
        }

        Key secretKey = null;
        try {
            secretKey = SymmetricKeyUtil.generateSecretKey();
        } catch (NoSuchAlgorithmException e) {
            throw new KeyProcessingException("비밀키 생성 중 오류가 발생했습니다.", e);
        }

        //2. 해시 및 전자서명 생성
        byte[] resumeBytes = null;
        byte[] signature = null;
        try {
            resumeBytes = resumeFile.getBytes();
            byte[] hash = HashUtil.calcHashVal(resumeBytes);
            signature = SignatureUtil.signData(privateKey, hash);
            Arrays.fill(hash, (byte) 0);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | IOException e) {
            throw new CryptoException("이력서 해시값 또는 전자서명 생성 중 오류가 발생했습니다.", e);
        }

        //3. SignedPayload 직렬화 및 암호화(암호문 생성)
        SignedPayload payload = new SignedPayload(resumeBytes, signature, publicKey);

        byte[] encryptedSignedPayload = null;
        try {
            byte[] signedPayloadBytes = SignedPayload.serializeToBytes(payload);
            encryptedSignedPayload = CipherUtil.encrypt(signedPayloadBytes, secretKey);

            Arrays.fill(resumeBytes, (byte) 0);
            Arrays.fill(signature, (byte) 0);
            Arrays.fill(signedPayloadBytes, (byte) 0);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException e){
            throw new CryptoException("암호문 생성 중 오류가 발생했습니다.", e);
        }

        //4. 기업 공개키로 비밀키 암호화 (전자봉투 생성)
        Company company = companyRepository.findByName(companyName)
                .orElseThrow(() -> new CompanyNotFoundException("해당되는 기업을 찾을 수 없습니다."));
        if (company.getPublicKey() == null) {
            throw new KeyProcessingException("공개키가 존재하지 않습니다.");
        }

        byte[] encryptedSecretKey = null;
        try {
            PublicKey companyPublicKey = AsymmetricKeyUtil.loadPublicKey(company.getPublicKey());
            encryptedSecretKey = CipherUtil.encrypt(secretKey.getEncoded(), companyPublicKey);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | IOException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException | ClassNotFoundException e) {
            throw new CryptoException("전자봉투 생성 중 오류가 발생했습니다", e);
        }

        //5. Envelope 직렬화 및 저장
        EncryptedEnvelope envelopeDto = new EncryptedEnvelope(encryptedSignedPayload, encryptedSecretKey);
        byte[] envelopeBytes = EncryptedEnvelope.serializeToBytes(envelopeDto);

        Envelope envelope = Envelope.builder()
                .envelopeData(envelopeBytes)
                .envelopeType(EnvelopeType.RESUME)
                .applicant(applicant)
                .company(company)
                .build();
        envelopeRepository.save(envelope);

        Arrays.fill(encryptedSignedPayload, (byte) 0);
        Arrays.fill(encryptedSecretKey, (byte) 0);
        Arrays.fill(envelopeBytes, (byte) 0);
    }

    public List<String> getCompanyNames(){
        List<String> companyNames = new ArrayList<>();
        companyRepository.findAll().forEach(company -> companyNames.add(company.getName()));
        return companyNames;
    }

    public List<Envelope> getReceivedResumes(String hrName){
        //채용 담당자 확인
        Member hr = memberRepository.findByName(hrName)
                .orElseThrow(() -> new MemberNotFoundException("해당되는 채용 담당자를 찾을 수 없습니다."));
        if (!Role.HR.equals(hr.getRole())) {
            throw new IllegalStateException("입력된 이름의 멤버는 채용 담당자 권한이 없습니다.");
        }

        //getResumes가 null이면 빈 리스트 반환
        return hr.getCompany().getEnvelopes().stream()
                .filter(envelope -> envelope.getEnvelopeType() == EnvelopeType.RESUME)
                .toList();
    }

    public byte[] verifyResume(Long envelopeId, String hrName) {
        //1. 채용 담당자 및 지원자 확인
        Envelope envelope = envelopeRepository.findById(envelopeId)
                .orElseThrow(() -> new EnvelopeNotFoundException("해당되는 전자봉투를 찾을 수 없습니다."));

        Member hr = memberRepository.findByName(hrName)
                .orElseThrow(() -> new MemberNotFoundException("해당되는 채용 담당자를 찾을 수 없습니다."));

        if (!Role.HR.equals(hr.getRole())) {
            throw new IllegalStateException("입력된 이름의 멤버는 채용 담당자 권한이 없습니다.");
        }

        //2. 기업의 개인키 가져오기
        Company company = hr.getCompany();

        if (company.getPrivateKey() == null) {
            throw new KeyProcessingException("개인키가 존재하지 않습니다.");
        }

        PrivateKey companyPrivateKey = null;
        try {
            companyPrivateKey = AsymmetricKeyUtil.loadPrivateKey(company.getPrivateKey());
        } catch (IOException | ClassNotFoundException e) {
            throw new KeyProcessingException("기업의 개인키를 불러오는 중 문제가 발생했습니다.", e);
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
        try {
            byte[] secretKeyBytes = CipherUtil.decrypt(decryptedEnvelope.getEncryptedSecretKey(), companyPrivateKey);
            secretKey = new SecretKeySpec(secretKeyBytes, SymmetricKeyUtil.getAlgorithm()); //byte[] -> Key로 변환
            Arrays.fill(secretKeyBytes, (byte) 0);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException e) {
            throw new KeyProcessingException("비밀키 복호화 중 문제가 발생했습니다.", e);
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
        try {
            byte[] calculatedHash = HashUtil.calcHashVal(payload.getContent());
            valid = SignatureUtil.verifyData(payload.getSenderPublicKey(), calculatedHash, payload.getSignature());
            Arrays.fill(calculatedHash, (byte) 0);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new CryptoException("해시값 또는 전자서명 검증 중 오류가 발생했습니다.", e);
        }

        if (!valid) {
            return new byte[0];
        }

        return payload.getContent();
    }
}
