package com.sylvester.bankapp.admin.service;

import com.sylvester.bankapp.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
