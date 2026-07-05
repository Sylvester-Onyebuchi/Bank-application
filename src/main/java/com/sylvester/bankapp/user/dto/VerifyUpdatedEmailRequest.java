package com.sylvester.bankapp.user.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyUpdatedEmailRequest(
        @NotBlank(message = "Verification Code is required")
        String code,
        @NotBlank(message = "RefreshToken is required")
        String refreshToken
) {
}
