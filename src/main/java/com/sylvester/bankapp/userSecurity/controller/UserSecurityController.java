package com.sylvester.bankapp.userSecurity.controller;


import com.sylvester.bankapp.userSecurity.dto.CreatePinRequest;
import com.sylvester.bankapp.userSecurity.service.SecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pin")
@RequiredArgsConstructor
public class UserSecurityController {

    private final SecurityService securityService;

    @PostMapping("/create")
    public ResponseEntity<?> createTransactionPin(@Valid @RequestBody CreatePinRequest request,
                                                  @AuthenticationPrincipal Jwt jwt){
        securityService.createPin(request.pin(),  jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update")
    public ResponseEntity<?> changeTransactionPin(@Valid @RequestBody CreatePinRequest request,
                                                  @AuthenticationPrincipal Jwt jwt){
        securityService.changePin(request.pin(),  jwt.getSubject());
        return ResponseEntity.ok().build();
    }

}