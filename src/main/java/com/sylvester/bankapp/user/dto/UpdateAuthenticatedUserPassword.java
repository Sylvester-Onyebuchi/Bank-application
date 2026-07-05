package com.sylvester.bankapp.user.dto;

public record UpdateAuthenticatedUserPassword(
        String oldPassword,
        String newPassword
) {
}
