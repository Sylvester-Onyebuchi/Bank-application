package com.sylvester.bankapp.user.comtroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.bankapp.support.TestJwtArgumentResolver;
import com.sylvester.bankapp.user.dto.UpdateAuthenticatedUserPassword;
import com.sylvester.bankapp.user.dto.UpdatePhoneNumberRequest;
import com.sylvester.bankapp.user.dto.UpdateUserInfo;
import com.sylvester.bankapp.user.dto.UserDto;
import com.sylvester.bankapp.user.dto.VerifyPhoneNumberRequest;
import com.sylvester.bankapp.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("access-token").header("alg", "none").subject("user-123").claim("sub", "user-123").build();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(authService))
                .setCustomArgumentResolvers(new TestJwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void phoneNumberEndpointsUseJwtAccessToken() throws Exception {
        VerifyPhoneNumberRequest verify = new VerifyPhoneNumberRequest("123456");
        UpdatePhoneNumberRequest update = new UpdatePhoneNumberRequest("+385911234567");

        mockMvc.perform(post("/api/v1/users/user/phone-number/send"))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code sent to your phone number"));

        mockMvc.perform(post("/api/v1/users/user/phone-number/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verify)))
                .andExpect(status().isOk())
                .andExpect(content().string("Phone number verified"));

        mockMvc.perform(put("/api/v1/users/user/phone-number/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(content().string("Phone verification code sent to your phone number"));

        verify(authService).sendPhoneVerificationCode("access-token");
        verify(authService).verifyPhoneNumber("access-token", verify);
        verify(authService).updatePhoneNumber("access-token", update);
    }

    @Test
    void profileAndPasswordEndpointsUseAuthenticatedUser() throws Exception {
        UpdateUserInfo update = new UpdateUserInfo("First", null, null, null, null, null, null, null);
        UpdateAuthenticatedUserPassword password = new UpdateAuthenticatedUserPassword("old", "new");
        when(authService.getUser("user-123")).thenReturn(new UserDto("username", "user@example.com", "First", "Last", "+385911234567", "Zagreb", "Croatia"));

        mockMvc.perform(put("/api/v1/users/user/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(content().string("User updated successfully"));

        mockMvc.perform(put("/api/v1/users/user/update-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(password)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));

        mockMvc.perform(get("/api/v1/users/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("username"))
                .andExpect(jsonPath("$.email").value("user@example.com"));

        verify(authService).updateUser("user-123", update);
        verify(authService).changePassword(password, "access-token");
        verify(authService).getUser("user-123");
    }
}
