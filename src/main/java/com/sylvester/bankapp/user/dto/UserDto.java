package com.sylvester.bankapp.user.dto;

public record UserDto(
        String username,
        String email,
        String firstname,
        String lastname,
        String phone,
        String city,
        String country
) {
}
