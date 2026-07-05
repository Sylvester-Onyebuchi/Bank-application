package com.sylvester.bankapp.user.comtroller;


import com.sylvester.bankapp.user.dto.*;
import com.sylvester.bankapp.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/user")
public class UserController {

    private final AuthService authService;

    @PostMapping("/phone-number/send")
    public ResponseEntity<?> sendPhoneVerificationCode(@AuthenticationPrincipal Jwt jwt) {
        String code = jwt.getTokenValue();
        authService.sendPhoneVerificationCode(code);
        return ResponseEntity.ok("Verification code sent to your phone number");
    }

    @PostMapping("/phone-number/verify")
    public ResponseEntity<?> verifyPhoneNumber(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VerifyPhoneNumberRequest request) {
        String accessToken = jwt.getTokenValue();
        authService.verifyPhoneNumber(accessToken, request);
        return ResponseEntity.ok("Phone number verified");
    }

    @PutMapping("/phone-number/update")
    public ResponseEntity<?> updatePhoneNumber(@Valid @RequestBody UpdatePhoneNumberRequest request, @AuthenticationPrincipal Jwt jwt) {
        String accessToken = jwt.getTokenValue();
        authService.updatePhoneNumber(accessToken, request);
        return ResponseEntity.ok("Phone verification code sent to your phone number");
    }

    @PutMapping("/profile/update")
    public ResponseEntity<?> updateUserData(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateUserInfo userData) {
        String userid = jwt.getSubject();
        authService.updateUser(userid,userData);
        return ResponseEntity.ok("User updated successfully");
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> changeAuthenticatedUserPassword(@RequestBody UpdateAuthenticatedUserPassword request, @AuthenticationPrincipal Jwt jwt) {
        authService.changePassword(request, jwt.getTokenValue());
        return ResponseEntity.ok("Password changed successfully");
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UserDto userDto = authService.getUser(jwt.getSubject());
        return ResponseEntity.ok(userDto);
    }
}
