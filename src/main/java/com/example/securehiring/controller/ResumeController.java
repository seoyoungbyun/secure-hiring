package com.example.securehiring.controller;

import com.example.securehiring.domain.Envelope;
import com.example.securehiring.domain.ResultNotification;
import com.example.securehiring.domain.dto.ResumeResultDTO;
import com.example.securehiring.domain.enums.EnvelopeType;
import com.example.securehiring.repository.EnvelopeRepository;
import com.example.securehiring.repository.ResultNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final EnvelopeRepository envelopeRepository;
    private final ResultNotificationRepository resultNotificationRepository;

    @GetMapping
    public List<ResumeResultDTO> getResumes() {
        // 이력서 타입인 Envelope만 필터링
        List<Envelope> resumeEnvelopes = envelopeRepository.findByEnvelopeType(EnvelopeType.RESUME);

        return resumeEnvelopes.stream().map(envelope -> {
            ResultNotification result = resultNotificationRepository.findByEnvelope(envelope).orElse(null);
            assert result != null;
            return new ResumeResultDTO(envelope, result);
        }).collect(Collectors.toList());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResumeResultDTO>> searchResume(@RequestParam String name) {
        List<Envelope> envelopes = envelopeRepository.findByEnvelopeTypeAndSenderName(EnvelopeType.RESUME, name);
        if (envelopes.isEmpty()) return ResponseEntity.notFound().build();

        List<ResumeResultDTO> resultList = envelopes.stream()
                .map(envelope -> {
                    ResultNotification result = resultNotificationRepository.findByEnvelope(envelope).orElse(null);
                    assert result != null;
                    return new ResumeResultDTO(envelope, result);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(resultList);
    }
}
