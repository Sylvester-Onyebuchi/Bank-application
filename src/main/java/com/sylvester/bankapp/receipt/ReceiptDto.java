package com.sylvester.bankapp.receipt;

import com.sylvester.bankapp.transaction.entity.TransactionStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptDto {

    private String transactionId;
    private String senderAccountNumber;
    private String senderName;
    private String recipientAccountNumber;
    private String recipientName;
    private BigDecimal amount;
    private String transactionType;
    private String transactionReference;
    private LocalDateTime createdDate;
    private TransactionStatus status;

}
