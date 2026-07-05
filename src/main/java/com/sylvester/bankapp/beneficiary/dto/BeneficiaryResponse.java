package com.sylvester.bankapp.beneficiary.dto;

public record BeneficiaryResponse(
        Long beneficiaryId,
        String name,
        String accountNumber
) {
}
