package com.sylvester.bankapp.rabbitmq.dto;

import java.math.BigDecimal;

public record TransferEvent(
        String transactionId,
        String senderAccountNumber,
        String senderName,
        String senderEmail,
        String recipientAccountNumber,
        String recipientName,
        String recipientEmail,
        BigDecimal amount,
        String transactionReference,
        String narration
) {}
