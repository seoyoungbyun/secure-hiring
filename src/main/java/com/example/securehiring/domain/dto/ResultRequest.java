package com.example.securehiring.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultRequest {
    private Long envelopeId;
    private String hrName;
    private String applicantName;
    private boolean result;
}
