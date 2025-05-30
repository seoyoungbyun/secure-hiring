package com.example.securehiring.domain.dto;

import com.example.securehiring.domain.Envelope;
import com.example.securehiring.domain.ResultNotification;
import lombok.Getter;

@Getter
public class ResumeResultDTO {
    private final Long envelopeId;
    private final String applicantName;
    private final boolean result; // true = 합격, false = 불합격

    public ResumeResultDTO(Envelope envelope, ResultNotification resultNotification) {
        this.envelopeId = envelope.getId();
        this.applicantName = envelope.getSender().getName(); // Member 이름
        this.result = resultNotification != null && resultNotification.isResult();
    }
}
