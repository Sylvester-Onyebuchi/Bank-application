package com.sylvester.bankapp.account.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(
        @NotBlank(message = "AccountType is required")
        String accountType

) {
}
