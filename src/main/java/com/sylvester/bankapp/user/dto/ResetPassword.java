package com.sylvester.bankapp.user.dto;

public record ResetPassword(String email, String code, String newPassword) {
}
