package com.example.securehiring.controller;

import com.example.securehiring.domain.Envelope;
import com.example.securehiring.domain.dto.ResultRequest;
import com.example.securehiring.exception.*;
import com.example.securehiring.service.ResultService;
import com.example.securehiring.service.ResumeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Controller
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrController {

    private final ResumeService resumeService;
    private final ResultService resultService;

    @GetMapping("/resumes/view")
    public String viewReceivedResumes(@RequestParam String hrName,
                                      @RequestParam(defaultValue = "resumes") String viewType,
                                      Model model) {
        try {
            List<Envelope> envelopes = resumeService.getReceivedResumes(hrName);
            model.addAttribute("envelopes", envelopes);
            model.addAttribute("hrName", hrName);
            return viewType + "/resumeList";
        } catch (MemberNotFoundException e) {
            System.out.println("회원 정보를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (IllegalStateException e) {
            System.out.println("불완전한 상태입니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/resumes/verify")
    public String verifyResume(@RequestParam Long envelopeId,
                               @RequestParam String hrName,
                               Model model) {
        try {
            byte[] result = resumeService.verifyResume(envelopeId, hrName);

            boolean verified = result.length > 0;
            model.addAttribute("verified", verified);
            model.addAttribute("envelopeId", envelopeId);
            model.addAttribute("hrName", hrName);

            return "resumes/resumeVerify";
        } catch (MemberNotFoundException e) {
            System.out.println("회원 정보를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (EnvelopeNotFoundException e) {
            System.out.println("전자봉투를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (IllegalStateException e) {
            System.out.println("불완전한 상태입니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (KeyProcessingException e) {
            System.out.println("키 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (CryptoException e) {
            System.out.println("암호화/복호화 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/resumes/download")
    public String downloadResume(@RequestParam Long envelopeId,
                                 @RequestParam String hrName,
                                 HttpServletResponse response,
                                 Model model) {
        try {
            byte[] resumeBytes = resumeService.verifyResume(envelopeId, hrName);
            if (resumeBytes.length == 0) {
                throw new SecurityException("전자서명 검증에 실패하여 이력서를 다운로드할 수 없습니다.");
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=verified_resume.pdf");
            response.setContentLength(resumeBytes.length);

            try (OutputStream os = response.getOutputStream()) {
                os.write(resumeBytes);
                os.flush();
            } catch (IOException e) {
                System.out.println("파일 전송 중 오류 발생: " + e.getMessage());
                e.printStackTrace();

                throw new IOException("이력서 다운로드 중 오류가 발생했습니다.", e);
            }

            return null;
        } catch (MemberNotFoundException e) {
            System.out.println("회원 정보를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (EnvelopeNotFoundException e) {
            System.out.println("전자봉투를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (IllegalStateException e) {
            System.out.println("불완전한 상태입니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (KeyProcessingException e) {
            System.out.println("키 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (CryptoException e) {
            System.out.println("암호화/복호화 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (SecurityException e) {
            System.out.println("전자서명 검증 실패: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (IOException e) {
            System.out.println("이력서 다운로드 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/results/generate")
    public String createResult(@ModelAttribute ResultRequest request, Model model) {
        try {
            resultService.createResult(request.getEnvelopeId(), request.getHrName(), request.isResult());
            return "results/evaluateSuccess";
        } catch (MemberNotFoundException e) {
            System.out.println("회원 정보를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (EnvelopeNotFoundException e) {
            System.out.println("전자봉투를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (IllegalStateException e) {
            System.out.println("불완전한 상태입니다: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (KeyProcessingException e) {
            System.out.println("키 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (CryptoException e) {
            System.out.println("암호화/복호화 오류 발생: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }
}
