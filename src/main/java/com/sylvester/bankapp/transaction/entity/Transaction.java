package com.sylvester.bankapp.transaction.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sylvester.bankapp.account.entity.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String transactionId;
    private BigDecimal amount;
    private String transactionType;
    private String transactionReference;
    private LocalDate createdDate;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    private String reverseOf;
    private String narration;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_account_id")
    private Account senderAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepient_account_id")
    private Account recipientAccount;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    private byte[] receiptPdf;
}
