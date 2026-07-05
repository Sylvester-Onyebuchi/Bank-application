package com.sylvester.bankapp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyPhoneNumberRequest(
        @NotBlank(message = "Verification Code is required")
        @Size(min = 6, max = 6, message = "The code must be exactly 6 numbers")
        String code
) {
}
