package com.sylvester.bankapp.user.dto;

public record LoginRequest(
        String email,
        String password
) {
}
