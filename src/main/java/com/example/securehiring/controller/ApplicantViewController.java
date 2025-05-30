package com.example.securehiring.controller;

import com.example.securehiring.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/applicant")
@RequiredArgsConstructor
public class ApplicantViewController {

    private final ResumeService resumeService;

    @GetMapping
    public String showApplicantPage() {
        return "applicant";
    }

    @GetMapping("/results/check")
    public String showResultPage() {
        return "results/checkResult";
    }

    @GetMapping("/resumes/upload")
    public String showUploadPage(Model model) {
        List<String> companyNames = resumeService.getCompanyNames();
        model.addAttribute("companyNames", companyNames);
        return "resumes/resumeUpload";
    }
}

