package com.example.securehiring.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeUploadRequest {
    private String applicantName;
    private String companyName;
}
