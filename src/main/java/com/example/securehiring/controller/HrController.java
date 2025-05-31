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
        } catch (MemberNotFoundException | IllegalStateException e) {
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

            model.addAttribute("verified", result != null);
            model.addAttribute("envelopeId", envelopeId);
            model.addAttribute("hrName", hrName);

            return "resumes/resumeVerify";
        }catch (MemberNotFoundException | EnvelopeNotFoundException | IllegalStateException |
                KeyProcessingException | CryptoException | SecurityException e){
            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/resumes/download")
    public void downloadResume(@RequestParam Long envelopeId,
                               @RequestParam String hrName,
                               HttpServletResponse response) throws Exception {
        byte[] resumeBytes = resumeService.verifyResume(envelopeId, hrName);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=verified_resume.pdf");
        response.setContentLength(resumeBytes.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(resumeBytes);
            os.flush();
        }
    }

    @PostMapping("/results/generate")
    public String createResult(@ModelAttribute ResultRequest request, Model model) {
        try {
            resultService.createResult(request.getEnvelopeId(), request.getHrName(), request.isResult());
            return "results/evaluateSuccess";
        } catch (MemberNotFoundException | IllegalStateException | EnvelopeNotFoundException |
                 KeyProcessingException | CryptoException e) {
            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }
}

