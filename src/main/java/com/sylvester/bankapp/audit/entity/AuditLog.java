package com.sylvester.bankapp.audit.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @SequenceGenerator(
            name = "audit_seq_gen",
            sequenceName = "audit_sequence",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_seq_gen")
    private Long id;
    private String actorId;
    private String targetId;
    private String action;
    private boolean success;
    private String failureReason;
    private String reason;
    private Instant createdDate;
}
