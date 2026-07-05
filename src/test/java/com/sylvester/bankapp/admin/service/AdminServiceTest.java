package com.sylvester.bankapp.admin.service;

import com.sylvester.bankapp.account.entity.Account;
import com.sylvester.bankapp.account.entity.AccountStatus;
import com.sylvester.bankapp.account.repository.AccountRepository;
import com.sylvester.bankapp.admin.dto.AdminUserResponse;
import com.sylvester.bankapp.admin.dto.ExtendRoleRequest;
import com.sylvester.bankapp.audit.dto.SaveAudit;
import com.sylvester.bankapp.audit.entity.AuditLog;
import com.sylvester.bankapp.audit.service.AuditLogService;
import com.sylvester.bankapp.transaction.entity.Transaction;
import com.sylvester.bankapp.transaction.entity.TransactionStatus;
import com.sylvester.bankapp.transaction.repository.TransactionRepository;
import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.user.entity.UserStatus;
import com.sylvester.bankapp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CognitoIdentityProviderClient cognito;

    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "userPoolId", "pool-id");
    }

    @Test
    void lockUserLocksUserAndWritesAuditLog() {
        User target = user("user-123", "Target", "User", "target@example.com", UserStatus.ACTIVE);
        User admin = user("admin-id", "Admin", "User", "admin@example.com", UserStatus.ACTIVE);
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(target));

        adminService.lockUser("target@example.com", "admin-id");

        ArgumentCaptor<SaveAudit> auditCaptor = ArgumentCaptor.forClass(SaveAudit.class);
        verify(userRepository).save(target);
        verify(auditLogService).saveLog(auditCaptor.capture());
        assertThat(target.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(auditCaptor.getValue().admin()).isEqualTo("admin");
        assertThat(auditCaptor.getValue().action()).isEqualTo("Locked User");
    }

    @Test
    void unlockUserThrowsWhenAdminIsLocked() {
        User target = user("user-123", "Target", "User", "target@example.com", UserStatus.LOCKED);
        User admin = user("admin-id", "Admin", "User", "admin@example.com", UserStatus.LOCKED);
        when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(target));
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.unLockUser("target@example.com", "admin-id"))
                .isInstanceOf(LockedException.class)
                .hasMessage("Admin is already locked");
    }

    @Test
    void reverseTransactionCreatesReversalUpdatesBalancesAndWritesAuditLog() {
        User admin = user("admin-id", "Admin", "User", "admin@example.com", UserStatus.ACTIVE);
        User sender = user("sender-id", "Sender", "User", "sender@example.com", UserStatus.ACTIVE);
        Account senderAccount = account(1L, "sender-account", sender, "500.00", AccountStatus.ACTIVE);
        Account recipientAccount = account(2L, "recipient-account", user("recipient-id", "Recipient", "User", "recipient@example.com", UserStatus.ACTIVE), "200.00", AccountStatus.ACTIVE);
        Transaction transaction = Transaction.builder()
                .transactionId("tx-123")
                .senderAccount(senderAccount)
                .recipientAccount(recipientAccount)
                .amount(new BigDecimal("75.00"))
                .status(TransactionStatus.SUCCESS)
                .build();

        when(transactionRepository.findByTransactionId("tx-123")).thenReturn(Optional.of(transaction));
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(admin));
        when(accountRepository.findByAccountNumber(senderAccount.getAccountNumber())).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber(recipientAccount.getAccountNumber())).thenReturn(Optional.of(recipientAccount));

        adminService.reverseTransaction("tx-123", "admin-id");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(accountRepository).save(senderAccount);
        verify(accountRepository).save(recipientAccount);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        verify(auditLogService).saveLog(any(SaveAudit.class));
        assertThat(senderAccount.getBalance()).isEqualByComparingTo("575.00");
        assertThat(recipientAccount.getBalance()).isEqualByComparingTo("125.00");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REVERSED);
        assertThat(transactionCaptor.getAllValues()).anySatisfy(reversal -> assertThat(reversal.getReverseOf()).isEqualTo("tx-123"));
    }

    @Test
    void findMethodsDelegateToRepositoriesWithPagingAndSorting() {
        Transaction transaction = Transaction.builder().transactionId("tx-1").build();
        AuditLog auditLog = AuditLog.builder().id(1L).action("Action").build();
        User user = user("user-123", "Target", "User", "target@example.com", UserStatus.ACTIVE);
        when(transactionRepository.findAll(any(Sort.class))).thenReturn(List.of(transaction));
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(transaction)));
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(auditLog)));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));

        assertThat(adminService.findAllTransactions()).containsExactly(transaction);
        assertThat(adminService.findTransactions().getContent()).containsExactly(transaction);
        assertThat(adminService.findAuditLogs().getContent()).containsExactly(auditLog);
        assertThat(adminService.findUsers().getContent()).containsExactly(user);
    }

    @Test
    void retryAfterFailureTransfersFundsAndMarksOriginalCompleted() {
        Account senderAccount = account(1L, "sender-account", null, "500.00", AccountStatus.ACTIVE);
        Account recipientAccount = account(2L, "recipient-account", null, "200.00", AccountStatus.ACTIVE);
        Transaction transaction = Transaction.builder()
                .transactionId("tx-123")
                .senderAccount(senderAccount)
                .recipientAccount(recipientAccount)
                .amount(new BigDecimal("75.00"))
                .status(TransactionStatus.FAILED)
                .build();

        when(transactionRepository.findByTransactionId("tx-123")).thenReturn(Optional.of(transaction));
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(user("admin-id", "Admin", "User", "admin@example.com", UserStatus.ACTIVE)));
        when(accountRepository.findByAccountNumber(senderAccount.getAccountNumber())).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber(recipientAccount.getAccountNumber())).thenReturn(Optional.of(recipientAccount));

        adminService.retryAfterFailure("tx-123", "admin-id");

        assertThat(senderAccount.getBalance()).isEqualByComparingTo("425.00");
        assertThat(recipientAccount.getBalance()).isEqualByComparingTo("275.00");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void lockAndUnlockAccountPersistStatusAndWriteAuditLog() {
        User admin = user("admin-id", "Admin", "User", "admin@example.com", UserStatus.ACTIVE);
        Account account = account(1L, "account-123", admin, "100.00", AccountStatus.ACTIVE);
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(admin));
        when(accountRepository.findByAccountNumber("account-123")).thenReturn(Optional.of(account));

        adminService.lockUserAccount("account-123", "admin-id");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.LOCKED);

        adminService.unLockUserAccount("account-123", "admin-id");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.LOCKED);

        verify(accountRepository, times(2)).save(account);
        verify(auditLogService, times(2)).saveLog(any(SaveAudit.class));
    }

    @Test
    void extendAndRemoveRoleCallCognitoAndWriteAudit() {
        ExtendRoleRequest request = new ExtendRoleRequest("user@example.com", "ADMIN");
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(user("admin-id", "Admin", "User", "admin@example.com", UserStatus.ACTIVE)));

        adminService.extendUserRole(request, "admin-id");
        adminService.removeRole(request, "admin-id");

        ArgumentCaptor<AdminAddUserToGroupRequest> addCaptor = ArgumentCaptor.forClass(AdminAddUserToGroupRequest.class);
        ArgumentCaptor<AdminRemoveUserFromGroupRequest> removeCaptor = ArgumentCaptor.forClass(AdminRemoveUserFromGroupRequest.class);
        verify(cognito).adminAddUserToGroup(addCaptor.capture());
        verify(cognito).adminRemoveUserFromGroup(removeCaptor.capture());
        assertThat(addCaptor.getValue().userPoolId()).isEqualTo("pool-id");
        assertThat(addCaptor.getValue().groupName()).isEqualTo("ADMIN");
        assertThat(removeCaptor.getValue().username()).isEqualTo("user@example.com");
        verify(auditLogService, times(2)).saveLog(any(SaveAudit.class));
    }

    @Test
    void getAdminsMapsCognitoUsersToResponses() {
        UserType admin = UserType.builder()
                .username("admin-user")
                .enabled(true)
                .userStatus("CONFIRMED")
                .attributes(AttributeType.builder().name("email").value("admin@example.com").build())
                .build();
        when(cognito.listUsersInGroup(any(ListUsersInGroupRequest.class)))
                .thenReturn(ListUsersInGroupResponse.builder().users(admin).build());

        List<AdminUserResponse> admins = adminService.getAdmins();

        assertThat(admins).containsExactly(new AdminUserResponse("admin-user", "admin@example.com", "CONFIRMED", true));
    }

    @Test
    void disableAccountRejectsNonZeroBalanceAndDisablesZeroBalanceAccount() {
        Account nonZero = account(1L, "account-123", user("user-123", "Target", "User", "target@example.com", UserStatus.ACTIVE), "100.00", AccountStatus.ACTIVE);
        Account zero = account(2L, "account-456", user("user-456", "Zero", "User", "zero@example.com", UserStatus.ACTIVE), "0.00", AccountStatus.ACTIVE);
        when(accountRepository.findByAccountNumber("account-123")).thenReturn(Optional.of(nonZero));
        when(accountRepository.findByAccountNumber("account-456")).thenReturn(Optional.of(zero));

        assertThatThrownBy(() -> adminService.disableAccount("account-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account balance cannot be disabled due to the balance not being 0");

        adminService.disableAccount("account-456");

        assertThat(zero.getStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(zero.getClosedAt()).isNotNull();
        verify(accountRepository).save(zero);
        verify(auditLogService).saveLog(any(SaveAudit.class));
    }

    @Test
    void disableUserDisablesCognitoUserAndZeroBalanceAccounts() {
        User admin = user("admin-id", "Admin", "User", "admin@example.com", UserStatus.ACTIVE);
        User target = user("user-123", "Target", "User", "target@example.com", UserStatus.ACTIVE);
        Account account = account(1L, "account-123", target, "0.00", AccountStatus.ACTIVE);
        when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(target));
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(admin));
        when(accountRepository.findAllByOwner_Email("target@example.com")).thenReturn(List.of(account));

        adminService.disableUser("target@example.com", "admin-id");

        ArgumentCaptor<AdminDisableUserRequest> disableCaptor = ArgumentCaptor.forClass(AdminDisableUserRequest.class);
        verify(cognito).adminDisableUser(disableCaptor.capture());
        verify(accountRepository).saveAll(List.of(account));
        verify(userRepository).save(target);
        assertThat(disableCaptor.getValue().username()).isEqualTo("target@example.com");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(target.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
    }

    private User user(String id, String firstName, String lastName, String email, UserStatus status) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .username(firstName.toLowerCase())
                .email(email)
                .status(status)
                .build();
    }

    private Account account(Long id, String accountNumber, User owner, String balance, AccountStatus status) {
        return Account.builder()
                .id(id)
                .accountNumber(accountNumber)
                .owner(owner)
                .balance(new BigDecimal(balance))
                .status(status)
                .build();
    }
}
