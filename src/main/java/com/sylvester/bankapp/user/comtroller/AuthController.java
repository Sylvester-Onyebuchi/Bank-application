package com.sylvester.bankapp.user.comtroller;



import com.sylvester.bankapp.user.dto.*;
import com.sylvester.bankapp.user.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserRequest;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthService authService;

    @PostMapping("/public/signup")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/public/resend")
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestBody RequestWithEmail request) {
        authService.resendCode(request);
        return ResponseEntity.ok("Verification code resent");
    }




    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request,
                                    HttpServletResponse response) {
        authService.logout(request.refreshToken());
        ResponseCookie accessCookie = ResponseCookie.from("refresh_token", "")
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(0)
                        .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/public/login")
    public ResponseEntity<TokenResponse> login (@RequestBody LoginRequest request, HttpServletResponse httpServletResponse) {
        TokenResponse response = authService.login(request);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", response.accessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofMinutes(10))
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(5))
                .build();
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok(response);

    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken ( @Valid @RequestBody RefreshTokenRequest request,
                                            @AuthenticationPrincipal Jwt jwt,
                                            HttpServletResponse httpServletResponse) {
       TokenResponse response = authService.refreshToken(request, jwt.getSubject());
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", response.accessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofMinutes(10))
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(5))
                .build();
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify/updated-email")
    public ResponseEntity<?> verifyUpdatedEmail(@Valid @RequestBody VerifyUpdatedEmailRequest request, @AuthenticationPrincipal Jwt jwt) {
        String accessToken = jwt.getTokenValue();
        authService.verifyUpdatedEmail(accessToken, request);
        return ResponseEntity.ok("Email verified");
    }

    @PostMapping("/public/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody RequestWithEmail request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("Email sent successfully");
    }

    @PutMapping("/public/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPassword resetPassword) {
        authService.confirmForgotPassword(resetPassword);
        return ResponseEntity.ok("Password reset successfully");
    }






}
