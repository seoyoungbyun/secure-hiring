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
        } catch (KeyProcessingException | CryptoException | CompanyNotFoundException e) {
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
            model.addAttribute("result", new String(Objects.requireNonNull(result), StandardCharsets.UTF_8));
            model.addAttribute("applicantName", applicantName);

            return "results/resultVerify";
        } catch (EnvelopeNotFoundException | MemberNotFoundException | KeyProcessingException |
                 CryptoException | SecurityException e) {
            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }
}
