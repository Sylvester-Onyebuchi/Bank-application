package com.sylvester.bankapp.transaction.dto;

import java.math.BigDecimal;

public record TransferRequest(
        String accountNumber,
        String recipientAccountNumber,
        String recipientName,
        BigDecimal amount,
        String narration,
        String pin) {
}
