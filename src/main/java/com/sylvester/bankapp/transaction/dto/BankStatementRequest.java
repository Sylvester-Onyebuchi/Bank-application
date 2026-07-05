package com.sylvester.bankapp.transaction.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BankStatementRequest(
        String accountNumber,
        String startDate,
        String endDate
) {
}
