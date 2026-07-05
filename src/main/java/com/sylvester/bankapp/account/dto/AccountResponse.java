package com.sylvester.bankapp.account.dto;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String accountNumber,
        String accountType,
        String accountHolderName,
        BigDecimal balance,
        String currency
) {
}
