package com.sylvester.bankapp.userSecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreatePinRequest(

        @NotBlank(message = "PIN is required")
        @Pattern(
                regexp = "\\d{4}",
                message = "PIN must be exactly 4 digits"
        )
        String pin
) {
}
