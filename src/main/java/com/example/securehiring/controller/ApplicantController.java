package com.example.securehiring.controller;

import com.example.securehiring.exception.CompanyNotFoundException;
import com.example.securehiring.exception.CryptoException;
import com.example.securehiring.exception.KeyProcessingException;
import com.example.securehiring.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/applicant")
@RequiredArgsConstructor
public class ApplicantController {

    private final ResumeService resumeService;

    @GetMapping
    public String showApplicantPage() {
        return "applicant";
    }

    @GetMapping("/resumes/upload")
    public String showUploadPage(Model model){
        List<String> companieNames = resumeService.getCompanyNames();
        model.addAttribute("companies", companieNames);
        return "resumes/resumeUpload";
    }

    @PostMapping(value = "/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadResume(@RequestParam String memberName,
                               @RequestParam String companyName,
                               @RequestParam MultipartFile resumeFile,
                               Model model) {
        try {
            resumeService.uploadResume(memberName, companyName, resumeFile);
        }catch (KeyProcessingException | CryptoException | CompanyNotFoundException e){
            model.addAttribute("message", e.getMessage());
            return "error";
        }
        return "resumes/submitSuccess";
    }
}

