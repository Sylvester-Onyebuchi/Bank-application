package com.sylvester.bankapp.rabbitmq.dto;

public record ForgotPasswordEvent(
        String email,
        String username,
        String code
) {
}
