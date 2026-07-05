package com.sylvester.bankapp.user.dto;


public record UpdateUserInfo(
       String firstname,
       String lastname,
       String username,
       String email,
       String address,
       String city,
       String country,
       String phone
) {
}
