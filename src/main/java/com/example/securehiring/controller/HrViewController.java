package com.example.securehiring.controller;

import com.example.securehiring.domain.dto.ResultRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrViewController {

    @GetMapping
    public String showHrPage() {
        return "hr";
    }

    @GetMapping("/resumes/check")
    public String showResumesHrLoginPage() {
        return "resumes/checkHr";
    }

    @GetMapping("/results/check")
    public String showResultsHrLoginPage() {
        return "results/checkHr";
    }

    @GetMapping("/results/evaluate")
    public String showResultPage(ResultRequest request, Model model) {
        model.addAttribute("request", request);
        return "results/evaluateResult";
    }
}

