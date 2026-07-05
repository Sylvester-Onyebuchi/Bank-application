package com.sylvester.bankapp.rabbitmq.dto;

public record StatementEvent(
        String email,
        String username,
        byte[] pdf
) {
}
