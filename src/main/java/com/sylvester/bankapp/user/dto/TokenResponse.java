package com.sylvester.bankapp.user.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String idToken,
        Integer expiresIn,
        String tokenType
) {
}
