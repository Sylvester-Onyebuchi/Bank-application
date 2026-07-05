package com.sylvester.bankapp.rabbitmq.dto;

public record VerificationCodeEvent(
        String email,
        String username,
        String code
) {
}
