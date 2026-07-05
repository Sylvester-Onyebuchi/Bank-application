package com.sylvester.bankapp.audit.service;

import com.sylvester.bankapp.audit.dto.SaveAudit;
import com.sylvester.bankapp.audit.entity.AuditLog;
import com.sylvester.bankapp.audit.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditRepository auditRepository;

    public void saveLog(SaveAudit saveAudit) {

       AuditLog auditLog =  new AuditLog();
       auditLog.setActorId(saveAudit.admin());
       auditLog.setAction(saveAudit.action());
       auditLog.setTargetId(saveAudit.targetUser());
       auditLog.setReason(saveAudit.reason());
       auditLog.setCreatedDate(Instant.now());
       auditLog.setSuccess(true);
       auditLog.setFailureReason(null);
       auditRepository.save(auditLog);
       log.info("AuditLog saved successfully");

    }

}
