package com.sylvester.bankapp.transaction.dto;


import com.sylvester.bankapp.transaction.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDto(
        String id,
         BigDecimal amount,
         String transactionType,
         String direction,
         String description,
         LocalDate createdDate,
        TransactionStatus status

) {
}
