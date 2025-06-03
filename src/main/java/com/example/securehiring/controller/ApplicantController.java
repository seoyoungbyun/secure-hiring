package com.example.securehiring.controller;

import com.example.securehiring.domain.Envelope;
import com.example.securehiring.domain.dto.ResumeUploadRequest;
import com.example.securehiring.exception.*;
import com.example.securehiring.service.ResultService;
import com.example.securehiring.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/applicant")
@RequiredArgsConstructor
public class ApplicantController {

    private final ResumeService resumeService;
    private final ResultService resultService;

    @PostMapping(value = "/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadResume(@ModelAttribute ResumeUploadRequest request,
                               @RequestParam MultipartFile resumeFile,
                               Model model) {
        try {
            resumeService.uploadResume(request.getApplicantName(), request.getCompanyName(), resumeFile);
            return "resumes/submitSuccess";
        } catch (CompanyNotFoundException e) {
            System.out.println("기업을 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (KeyProcessingException e) {
            System.out.println("키 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (CryptoException e) {
            System.out.println("암호화/복호화 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/results/view")
    public String viewReceivedResults(@RequestParam String applicantName,
                                      Model model) {
        try {
            List<Envelope> envelopes = resultService.getReceivedResults(applicantName);

            model.addAttribute("envelopes", envelopes);
            model.addAttribute("applicantName", applicantName);

            return "results/resultList";
        } catch (MemberNotFoundException e) {
            System.out.println("지원자를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/results/verify")
    public String verifyResult(@RequestParam Long envelopeId,
                               @RequestParam String applicantName,
                               Model model) {
        try {
            byte[] result = resultService.verifyResult(envelopeId, applicantName);

            boolean verified = result.length > 0;
            model.addAttribute("verified", verified);
            model.addAttribute("result", new String(result, StandardCharsets.UTF_8));
            model.addAttribute("applicantName", applicantName);

            return "results/resultVerify";
        } catch (EnvelopeNotFoundException e) {
            System.out.println("전자봉투를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (MemberNotFoundException e) {
            System.out.println("지원자를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (KeyProcessingException e) {
            System.out.println("키 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (CryptoException e) {
            System.out.println("암호화/복호화 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }
}
