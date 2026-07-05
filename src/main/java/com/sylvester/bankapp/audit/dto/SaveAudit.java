package com.sylvester.bankapp.audit.dto;

public record SaveAudit(
        String admin, String action, String targetUser, String reason
) {
}
