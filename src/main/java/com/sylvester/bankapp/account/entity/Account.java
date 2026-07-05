package com.sylvester.bankapp.account.entity;

import com.sylvester.bankapp.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Account {
    @Id
    @SequenceGenerator(
            name = "account_seq_gen",
            sequenceName = "account_sequence",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq_gen")
    private Long id;
    private BigDecimal balance;
    @Column(unique = true)
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    private CurrencyType currency;
    @Enumerated(EnumType.STRING)
    private AccountType accountTypes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;
    private LocalDateTime closedAt;
    @Version
    private Long version;
    private Instant createdDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;
}
