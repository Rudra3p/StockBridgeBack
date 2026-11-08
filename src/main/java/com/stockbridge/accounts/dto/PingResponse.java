package com.stockbridge.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PingResponse {
    private String message;
    private String role;
    private String userEmail;
    private Instant timestamp;
}
