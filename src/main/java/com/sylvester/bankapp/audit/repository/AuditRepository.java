package com.sylvester.bankapp.audit.repository;

import com.sylvester.bankapp.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends JpaRepository<AuditLog, Long> {
}
