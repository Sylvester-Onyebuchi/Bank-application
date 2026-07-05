package com.sylvester.bankapp.account.entity;

import com.sylvester.bankapp.transaction.entity.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @Column(unique = true, nullable = false)
    private String key;

    private String transactionId;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private LocalDateTime createdAt;

    private String requestHash;
}
