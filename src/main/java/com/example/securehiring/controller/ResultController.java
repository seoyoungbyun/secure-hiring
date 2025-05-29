package com.example.securehiring.controller;

import com.example.securehiring.domain.Member;
import com.example.securehiring.domain.dto.ResultRequest;
import com.example.securehiring.domain.dto.ResultResponse;
import com.example.securehiring.exception.MemberNotFoundException;
import com.example.securehiring.repository.MemberRepository;
import com.example.securehiring.service.ResultGeneratorService;
import com.example.securehiring.service.ResultViewerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultGeneratorService resultGeneratorService;
    private final ResultViewerService resultViewerService;
    private final MemberRepository memberRepository;

    @PostMapping("/generate/{envelopeId}")
    public String createResult(@PathVariable Long envelopeId,
                               @RequestBody ResultRequest request) {
        resultGeneratorService.createResult(envelopeId, request);
        return "success";
    }

    @GetMapping("/view/{applicantName}")
    public ResponseEntity<List<ResultResponse>> viewResults(@PathVariable String applicantName) {
        Member member = memberRepository.findByName(applicantName)
                .orElseThrow(() -> new MemberNotFoundException("해당되는 채용 담당자를 찾을 수 없습니다."));

        List<ResultResponse> results = resultViewerService.viewResultsByApplicant(member);
        return ResponseEntity.ok(results);
    }
}