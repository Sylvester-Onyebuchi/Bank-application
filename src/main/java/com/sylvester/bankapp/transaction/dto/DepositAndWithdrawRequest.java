package com.sylvester.bankapp.transaction.dto;

import java.math.BigDecimal;

public record DepositAndWithdrawRequest(
        String accountNumber,
        BigDecimal amount
) {
}
