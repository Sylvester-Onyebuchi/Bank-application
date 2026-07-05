package com.sylvester.bankapp.user.comtroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.bankapp.support.TestJwtArgumentResolver;
import com.sylvester.bankapp.user.dto.CreateUserRequest;
import com.sylvester.bankapp.user.dto.LoginRequest;
import com.sylvester.bankapp.user.dto.LogoutRequest;
import com.sylvester.bankapp.user.dto.RefreshTokenRequest;
import com.sylvester.bankapp.user.dto.RequestWithEmail;
import com.sylvester.bankapp.user.dto.ResetPassword;
import com.sylvester.bankapp.user.dto.TokenResponse;
import com.sylvester.bankapp.user.dto.VerifyUpdatedEmailRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("access-token").header("alg", "none").subject("user-123").claim("sub", "user-123").build();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setCustomArgumentResolvers(new TestJwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void signupReturnsCreatedAndCallsService() throws Exception {
        CreateUserRequest request = createUserRequest();

        mockMvc.perform(post("/api/v1/auth/public/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        verify(authService).registerUser(request);
    }

    @Test
    void resendAndForgotPasswordPublicEndpointsCallService() throws Exception {
        RequestWithEmail request = new RequestWithEmail("user@example.com");

        mockMvc.perform(post("/api/v1/auth/public/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code resent"));

        mockMvc.perform(post("/api/v1/auth/public/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Email sent successfully"));

        verify(authService).resendCode(request);
        verify(authService).forgotPassword(request);
    }

    @Test
    void loginAndRefreshReturnTokenResponses() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "Password1!");
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("refresh-token");
        when(authService.login(loginRequest)).thenReturn(new TokenResponse("access", "refresh", "id", 3600, "Bearer"));
        when(authService.refreshToken(refreshRequest, "user-123")).thenReturn(new TokenResponse("access-2", "refresh-2", "id-2", 3600, "Bearer"));

        mockMvc.perform(post("/api/v1/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-2"));

        verify(authService).login(loginRequest);
        verify(authService).refreshToken(refreshRequest, "user-123");
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        LogoutRequest request = new LogoutRequest("refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).logout("refresh-token");
    }

    @Test
    void resetPasswordAndVerifyUpdatedEmailCallService() throws Exception {
        ResetPassword reset = new ResetPassword("user@example.com", "123456", "newPassword123");
        VerifyUpdatedEmailRequest verifyEmail = new VerifyUpdatedEmailRequest("123456", "refresh-token");

        mockMvc.perform(put("/api/v1/auth/public/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successfully"));

        mockMvc.perform(post("/api/v1/auth/verify/updated-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyEmail)))
                .andExpect(status().isOk())
                .andExpect(content().string("Email verified"));

        verify(authService).confirmForgotPassword(reset);
        verify(authService).verifyUpdatedEmail("access-token", verifyEmail);
    }

    private CreateUserRequest createUserRequest() {
        return new CreateUserRequest("Sylvester", "Onah", "middle", "user@example.com", "Password1!", "123 Main Street", "Zagreb", "Croatia", "+385911234567");
    }
}
