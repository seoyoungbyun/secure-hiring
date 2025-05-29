package com.example.securehiring.domain.dto;

import com.example.securehiring.domain.ResultNotification;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ResultResponse {
    private final String resultMessage;
    private ResultNotification employmentResult;

    public ResultResponse(String resultMessage) {
        this.resultMessage = resultMessage;
    }
}
