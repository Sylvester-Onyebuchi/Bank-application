package com.sylvester.bankapp.admin.controller;



import com.sylvester.bankapp.admin.dto.LockAndUnlockAccount;
import com.sylvester.bankapp.admin.dto.RevertTransaction;
import com.sylvester.bankapp.admin.service.AdminService;
import com.sylvester.bankapp.admin.dto.ExtendRoleRequest;
import com.sylvester.bankapp.user.dto.RequestWithEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/lock-user")
    public ResponseEntity<?> lockUser(@RequestBody RequestWithEmail request, @AuthenticationPrincipal Jwt jwt) {
        adminService.lockUser(request.email(), jwt.getSubject());
        return ResponseEntity.ok("User locked successfully");
    }

    @PostMapping("/unlock-user")
    public ResponseEntity<?> unlockUser(@RequestBody RequestWithEmail request, @AuthenticationPrincipal Jwt jwt) {
        adminService.unLockUser(request.email(), jwt.getSubject());
        return ResponseEntity.ok("User unlocked successfully");
    }

    @PostMapping("/lock-account")
    public ResponseEntity<?> lockUserAccount(@RequestBody LockAndUnlockAccount request, @AuthenticationPrincipal Jwt jwt) {
        adminService.lockUserAccount(request.accountNumber(), jwt.getSubject());
        return ResponseEntity.ok("User account locked successfully");
    }

    @PostMapping("/unlock-account")
    public ResponseEntity<?> unLockUserAccount(@RequestBody LockAndUnlockAccount request, @AuthenticationPrincipal Jwt jwt) {
        adminService.unLockUserAccount(request.accountNumber(),jwt.getSubject());
        return ResponseEntity.ok("User account unlocked successfully");
    }

    @PostMapping("/reverse")
    public ResponseEntity<?> revertTransaction(@RequestBody RevertTransaction transaction, @AuthenticationPrincipal Jwt jwt) {
        adminService.reverseTransaction(transaction.transactionId(), jwt.getSubject());
        return ResponseEntity.ok("Transaction revert successfully");
    }

    @PostMapping("/update-role")
    public ResponseEntity<?> updateUserRole(@RequestBody ExtendRoleRequest request, @AuthenticationPrincipal Jwt jwt) {
        adminService.extendUserRole(request,jwt.getSubject());
        return ResponseEntity.ok("Role updated successfully");
    }

    @DeleteMapping("/remove-role")
    public ResponseEntity<?> removeRole(@RequestBody ExtendRoleRequest request, @AuthenticationPrincipal Jwt jwt) {
        adminService.removeRole(request, jwt.getSubject());
        return ResponseEntity.ok("Role removed successfully");
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@RequestParam String accountNumber) {
        adminService.disableAccount(accountNumber);
        return ResponseEntity.ok("Account deleted successfully");
    }

    @DeleteMapping("/delete-user/{email}")
    public ResponseEntity<?> disableUser(@PathVariable String email, @AuthenticationPrincipal Jwt jwt) {
        adminService.disableUser(email, jwt.getSubject());
        return ResponseEntity.ok("Account deleted successfully");
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> allTransactions() {
        return ResponseEntity.ok(adminService.findTransactions());
    }

    @GetMapping("/users")
    public ResponseEntity<?> allUsers() {
        return ResponseEntity.ok(adminService.findUsers());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> allAuditLogs() {
        return ResponseEntity.ok(adminService.findAuditLogs());
    }


    @GetMapping("/all")
    public ResponseEntity<?> findAllAdmins() {
        return ResponseEntity.ok(adminService.getAdmins());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {

        List<String> groups = jwt.getClaimAsStringList("cognito:groups");

        return ResponseEntity.ok(groups);
    }
    
}
