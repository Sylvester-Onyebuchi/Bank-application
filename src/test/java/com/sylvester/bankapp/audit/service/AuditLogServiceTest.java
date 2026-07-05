package com.sylvester.bankapp.audit.service;

import com.sylvester.bankapp.audit.dto.SaveAudit;
import com.sylvester.bankapp.audit.entity.AuditLog;
import com.sylvester.bankapp.audit.repository.AuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void saveLogMapsDtoToAuditEntity() {
        auditLogService.saveLog(new SaveAudit("admin", "LOCK", "user", "reason"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getActorId()).isEqualTo("admin");
        assertThat(captor.getValue().getAction()).isEqualTo("LOCK");
        assertThat(captor.getValue().getTargetId()).isEqualTo("user");
        assertThat(captor.getValue().getReason()).isEqualTo("reason");
        assertThat(captor.getValue().isSuccess()).isTrue();
        assertThat(captor.getValue().getCreatedDate()).isNotNull();
    }
}
