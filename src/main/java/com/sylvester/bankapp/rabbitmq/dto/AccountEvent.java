package com.sylvester.bankapp.rabbitmq.dto;

public record AccountEvent(
        String email,
        String firstname,
        String accountNumber,
        String accountName,
        String accountType
) {
}
