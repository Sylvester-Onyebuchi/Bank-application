package com.sylvester.bankapp.account.controller;



import com.sylvester.bankapp.account.dto.CreateAccountRequest;
import com.sylvester.bankapp.account.entity.Account;
import com.sylvester.bankapp.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<?> createAccount(@Valid @RequestBody CreateAccountRequest request, @AuthenticationPrincipal Jwt jwt){
        String userId = jwt.getClaimAsString("sub");
        accountService.createAccount(request,userId);
        return ResponseEntity.ok("Account created successfully");
    }

    @GetMapping("/all")
    public ResponseEntity<List<Account>> findAllAccounts(){
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/me")
    public ResponseEntity<?> findAllUsersAccounts(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(accountService.getAllUsersAccounts(jwt.getSubject()));
    }
}
