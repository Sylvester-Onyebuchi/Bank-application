package com.sylvester.bankapp.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePhoneNumberRequest(

        @NotBlank(message = "Phone number is required")
        String phone_number
) {
}
