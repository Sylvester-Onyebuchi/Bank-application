package com.sylvester.bankapp.admin.service;



import com.sylvester.bankapp.account.entity.Account;
import com.sylvester.bankapp.account.entity.AccountStatus;
import com.sylvester.bankapp.account.repository.AccountRepository;
import com.sylvester.bankapp.admin.dto.AdminUserResponse;
import com.sylvester.bankapp.audit.dto.SaveAudit;
import com.sylvester.bankapp.audit.entity.AuditLog;
import com.sylvester.bankapp.audit.service.AuditLogService;
import com.sylvester.bankapp.exception.NotFoundException;
import com.sylvester.bankapp.transaction.entity.Transaction;
import com.sylvester.bankapp.transaction.entity.TransactionStatus;
import com.sylvester.bankapp.transaction.repository.TransactionRepository;
import com.sylvester.bankapp.admin.dto.ExtendRoleRequest;
import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.user.entity.UserStatus;
import com.sylvester.bankapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AccountRepository accountRepository;
    private final CognitoIdentityProviderClient cognito;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;


    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public List<Transaction> findAllTransactions(){
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate").and(Sort.by(Sort.Direction.ASC, "createdDate"));
        return transactionRepository.findAll(sort);

    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public Page<Transaction> findTransactions(){
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdDate"));
        return transactionRepository.findAll(pageRequest);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLog> findAuditLogs(){
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdDate"));
        return auditLogRepository.findAll(pageRequest);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> findUsers(){
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "username"));
        return userRepository.findAll(pageRequest);
    }


    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void lockUser(String email, String adminId){

        var admin = userRepository.findById(adminId).orElseThrow(
                () -> new NotFoundException("Admin not found")
        );

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("User not found")
        );

        if (user.getStatus() == UserStatus.LOCKED) {
            return;
        }
        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);
        log.info("User {} has been locked", user.getUsername());
        SaveAudit saveAudit = new SaveAudit(
                admin.getUsername(), "Locked User", user.getUsername(), "Unverified action going in the user account"
        );
        auditLogService.saveLog(saveAudit);

    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void retryAfterFailure(String transactionId, String adminId){
        Transaction transaction = transactionRepository.findByTransactionId(transactionId).orElseThrow(
                () -> new NotFoundException("Transaction not found")
        );
       userRepository.findById(adminId).orElseThrow(
                () -> new NotFoundException("Admin not found")
        );
        if (transaction.getStatus() != TransactionStatus.FAILED){
            throw new IllegalStateException("Only for failed transactions");
        }

        var from = accountRepository.findByAccountNumber(transaction.getSenderAccount().getAccountNumber()).orElseThrow(
                () -> new NotFoundException("Account not found")
        );
        Account to = accountRepository.findByAccountNumber(transaction.getRecipientAccount().getAccountNumber()).orElseThrow(
                () -> new NotFoundException("Account not found")
        );
        if (from.getBalance().compareTo(transaction.getAmount()) <= 0){
            throw new RuntimeException("Insufficient funds");
        }
        from.setBalance(from.getBalance().subtract(transaction.getAmount()));
        to.setBalance(to.getBalance().add(transaction.getAmount()));
        accountRepository.save(from);
        accountRepository.save(to);
        Transaction reversed = Transaction.builder()
                .amount(transaction.getAmount())
                .senderAccount(from)
                .recipientAccount(to)
                .status(TransactionStatus.SUCCESS)
                .reverseOf(transactionId)
                .createdDate(LocalDate.now())
                .build();
        transactionRepository.save(reversed);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

    }


    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void reverseTransaction(String transactionId, String adminId){

        Transaction transaction = transactionRepository.findByTransactionId(transactionId).orElseThrow(
                () -> new NotFoundException("Transaction not found")
        );

        var adminuser = userRepository.findById(adminId).orElseThrow(
                () -> new NotFoundException("Admin not found")
        );

        if (adminuser.getStatus().equals(UserStatus.LOCKED)) {
            throw new LockedException("Admin is locked");
        }

        if (transaction.getStatus() != TransactionStatus.SUCCESS){
            throw new IllegalStateException("Only for successful transactions");
        }
        if (transaction.getReverseOf() != null){
            throw new IllegalStateException("Transaction is already reversed");
        }

        var  from = accountRepository.
                findByAccountNumber(transaction.getSenderAccount().getAccountNumber()).orElseThrow(
                        () -> new NotFoundException("Account not found")
                );
        Account to = accountRepository.
                findByAccountNumber(transaction.getRecipientAccount().getAccountNumber()).orElseThrow(
                        () -> new NotFoundException("Account not found")
                );

        if (from.getBalance().compareTo(transaction.getAmount()) <= 0){
            throw new RuntimeException("Insufficient funds");
        }

        from.setBalance(from.getBalance().add(transaction.getAmount()));
        to.setBalance(to.getBalance().subtract(transaction.getAmount()));
        accountRepository.save(from);
        accountRepository.save(to);
        Transaction reversed = Transaction.builder()
                .amount(transaction.getAmount())
                .senderAccount(from)
                .recipientAccount(to)
                .status(TransactionStatus.SUCCESS)
                .reverseOf(transactionId)
                .createdDate(LocalDate.now())
                .build();
        transactionRepository.save(reversed);
        transaction.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(transaction);

        SaveAudit saveAudit = new SaveAudit(
                adminuser.getUsername(), "REVERSED A TRANSACTION", transaction.getTransactionId(),"The User "+from.getOwner().getUsername()+" requested for a reversed transaction"
        );
        auditLogService.saveLog(saveAudit);


    }


    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void unLockUser(String email, String adminId){

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("User not found")
        );

        var adminUser = userRepository.findById(adminId).orElseThrow(
                () -> new NotFoundException("Admin not found")
        );
        if (adminUser.getStatus().equals(UserStatus.LOCKED)) {
            throw new LockedException("Admin is already locked");
        }

        if (user.getStatus().equals(UserStatus.LOCKED)) {
            throw new LockedException("User is already locked");
        }
        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);
        log.info("User {} has been unlocked", user.getUsername());
        SaveAudit saveAudit = new SaveAudit(
                adminUser.getUsername(), "UnLocked User", user.getUsername(), "Unlocked user after some verification"
        );
        auditLogService.saveLog(saveAudit);

    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void lockUserAccount(String accountNumber, String adminId){

        var adminUser = userRepository.findById(adminId).orElseThrow(
                () -> new NotFoundException("Admin not found")
        );

        if (adminUser.getStatus().equals(UserStatus.LOCKED)) {
            throw new LockedException("Admin is already locked");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(
                () -> new NotFoundException("Account not found")
        );

        account.setStatus(AccountStatus.LOCKED);
        accountRepository.save(account);
        SaveAudit saveAudit = new SaveAudit(
                adminUser.getUsername(), "locked user account", account.getAccountNumber(), "There was a suspicious transaction that happened"
        );
        auditLogService.saveLog(saveAudit);
    }


    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void unLockUserAccount(String accountNumber, String adminId){


        var adminUser = userRepository.findById(adminId).orElseThrow(
                () -> new NotFoundException("Admin not found")
        );

        if (adminUser.getStatus().equals(UserStatus.LOCKED)) {
            throw new LockedException("Admin is already locked");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(
                () -> new NotFoundException("Account not found")
        );

        account.setStatus(AccountStatus.LOCKED);
        accountRepository.save(account);
        SaveAudit saveAudit = new SaveAudit(
                adminUser.getFullName(), "Unlocked user account", account.getAccountNumber(), "The suspicious transaction has been verified"
        );
        auditLogService.saveLog(saveAudit);
    }


    @PreAuthorize("hasRole('ADMIN')")
    public void extendUserRole(ExtendRoleRequest request, String adminId){
        try {
            var admin = userRepository.findById(adminId).orElseThrow(
                    () -> new NotFoundException("Admin not found")
            );
            AdminAddUserToGroupRequest groupRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(request.email())
                    .groupName(request.role())
                    .build();
            cognito.adminAddUserToGroup(groupRequest);
            SaveAudit saveAudit = new SaveAudit(admin.getFullName(), "Added user role "+request.role(), request.email(), "Added user role");
            auditLogService.saveLog(saveAudit);
        } catch (RuntimeException e) {
            System.out.println("Failed to add user to group: " + e.getMessage());
            throw new RuntimeException("Something went wrong", e);
        }

    }

    @PreAuthorize("hasRole('ADMIN')")
    public void removeRole(ExtendRoleRequest request, String adminId) {

        try {
            var admin = userRepository.findById(adminId).orElseThrow(
                    () -> new NotFoundException("Admin not found")
            );
            AdminRemoveUserFromGroupRequest removeUserFromGroupRequest =
                    AdminRemoveUserFromGroupRequest.builder()
                            .userPoolId(userPoolId)
                            .username(request.email())
                            .groupName(request.role())
                            .build();

            cognito.adminRemoveUserFromGroup(removeUserFromGroupRequest);
            SaveAudit saveAudit = new SaveAudit(admin.getFullName(), "Removed user role "+request.role(), request.email(), "Remove user role");
            auditLogService.saveLog(saveAudit);
        } catch (RuntimeException e) {
            System.out.println("Failed to remove user from group: " + e.getMessage());
            throw new RuntimeException("Failed to remove user from group",e);
        }
    }


    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminUserResponse> getAdmins() {

        List<UserType> admins = new ArrayList<>();

        String token = null;

        do {
            ListUsersInGroupResponse response = cognito.listUsersInGroup(
                    ListUsersInGroupRequest.builder()
                            .userPoolId(userPoolId)
                            .groupName("ADMIN")
                            .nextToken(token)
                            .build()
                    );

            admins.addAll(response.users());
            token = response.nextToken();

        } while (token != null);

        return admins.stream().map(user -> {
            String email = user.attributes().stream()
                    .filter(attr -> attr.name().equals("email"))
                    .map(AttributeType::value)
                    .findFirst()
                    .orElse("No Email");

            return new AdminUserResponse(
                    user.username(),
                    email,
                    user.userStatusAsString(),
                    user.enabled()
            );
        }).toList();

    }


    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void disableUser(String email, String adminId){
        try {
            var user = userRepository.findByEmail(email).orElseThrow(
                    () -> new NotFoundException("User not found")
            );
            var admin = userRepository.findById(adminId).orElseThrow(
                    () -> new NotFoundException("Admin not found")
            );
            List<Account> accounts = accountRepository.findAllByOwner_Email(email);
            boolean hasBalance = accounts.stream().anyMatch(
                    account -> account.getBalance().compareTo(BigDecimal.ZERO) != 0);
            if (hasBalance){
                throw new RuntimeException("User account cannot be deleted right now because the balance is not 0");
            }
            for(Account account : accounts){
                account.setStatus(AccountStatus.DISABLED);
                account.setClosedAt(LocalDateTime.now());
            }
            accountRepository.saveAll(accounts);
            AdminDisableUserRequest request = AdminDisableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .build();
            cognito.adminDisableUser(request);
            user.setStatus(UserStatus.DEACTIVATED);
            userRepository.save(user);
            SaveAudit saveAudit = new SaveAudit(admin.getFullName(), "Disabled user", user.getUsername(), "Disabled user as the user requested after some verification");
            auditLogService.saveLog(saveAudit);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void disableAccount(String accountNumber){

        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(
                () -> new NotFoundException("Account not found")
        );
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0){
            throw new RuntimeException("Account balance cannot be disabled due to the balance not being 0");
        }
        account.setStatus(AccountStatus.DISABLED);
        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);
        SaveAudit saveAudit = new SaveAudit(
                account.getOwner().getFullName(), "Account disable", account.getOwner().getUsername(), "Account has been disabled"
        );
        auditLogService.saveLog(saveAudit);
    }

}
