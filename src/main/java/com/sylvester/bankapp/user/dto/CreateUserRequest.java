package com.sylvester.bankapp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record CreateUserRequest(
        @NotBlank(message = "Firstname is required")
        @Length(min = 3, max = 20, message = "It must be within 6 to 15 characters")
        String firstname,
        @NotBlank(message = "Lastname is required")
        @Length(min = 3, max = 20, message = "It must be within 6 to 15 characters")
        String lastname,
        @NotBlank(message = "Username is required")
        @Length(min = 6, max = 15, message = "It must be within 6 to 15 characters")
        String username,
        @NotBlank(message = "Email is required")
        @Email(message = "It must be in correct email format")
        String email,
        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&()#^+=_\\-])[A-Za-z\\d@$!%*?&()#^+=_\\-]{8,}$",
                message = """
                  Password must:
                  - Be at least 8 characters long
                  - Contain at least one uppercase letter
                  - Contain at least one lowercase letter
                  - Contain at least one number
                  - Contain at least one special character
                  """
        )
        String password,
        @NotBlank(message = "Address is required")
        String address,
        @NotBlank(message = "City is required")
        @Length(min = 3, max = 15, message = "It must be within 3 to 15 characters")
        String city,
        @NotBlank(message = "Country is required")
        @Length(min = 4, max = 15, message = "It must be within 4 to 15 characters")
        String country,
        @NotBlank(message = "Phone is required")
        @Length(min = 10, max = 15, message = "It must be within 10 to 15 characters")
        String phone
) {
}
