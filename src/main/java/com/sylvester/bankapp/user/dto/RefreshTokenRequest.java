package com.sylvester.bankapp.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Token is required")
        String token
) {
}
