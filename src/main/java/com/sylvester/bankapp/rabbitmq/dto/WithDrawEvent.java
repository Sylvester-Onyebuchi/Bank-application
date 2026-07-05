package com.sylvester.bankapp.rabbitmq.dto;

import java.math.BigDecimal;

public record WithDrawEvent(
        String accountName,
        String username,
        String email,
        BigDecimal amount
) {
}
