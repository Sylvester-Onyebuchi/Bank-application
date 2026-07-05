package com.sylvester.bankapp.rabbitmq.dto;

import java.math.BigDecimal;

public record DepositEvent(
        String accountName,
        String username,
        String email,
        BigDecimal amount
) {
}
