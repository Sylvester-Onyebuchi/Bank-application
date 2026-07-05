package com.sylvester.bankapp.account.service;

import com.sylvester.bankapp.account.dto.AccountResponse;
import com.sylvester.bankapp.account.dto.CreateAccountRequest;
import com.sylvester.bankapp.account.entity.Account;
import com.sylvester.bankapp.account.entity.AccountStatus;
import com.sylvester.bankapp.account.entity.AccountType;
import com.sylvester.bankapp.account.entity.CurrencyType;
import com.sylvester.bankapp.account.repository.AccountRepository;
import com.sylvester.bankapp.audit.dto.SaveAudit;
import com.sylvester.bankapp.audit.service.AuditLogService;
import com.sylvester.bankapp.rabbitmq.Publisher;
import com.sylvester.bankapp.rabbitmq.dto.AccountEvent;
import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Publisher publisher;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountPersistsAccountPublishesEventAndWritesAuditLog() {
        User user = user();
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(accountRepository.existsByOwner_IdAndAccountTypes("user-123", AccountType.SAVINGS)).thenReturn(false);

        accountService.createAccount(new CreateAccountRequest("SAVINGS"), "user-123");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        ArgumentCaptor<AccountEvent> eventCaptor = ArgumentCaptor.forClass(AccountEvent.class);
        ArgumentCaptor<SaveAudit> auditCaptor = ArgumentCaptor.forClass(SaveAudit.class);

        verify(accountRepository).save(accountCaptor.capture());
        verify(publisher).publishAccount(eventCaptor.capture());
        verify(auditLogService).saveLog(auditCaptor.capture());

        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getOwner()).isEqualTo(user);
        assertThat(savedAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedAccount.getCurrency()).isEqualTo(CurrencyType.EURO);
        assertThat(savedAccount.getAccountTypes()).isEqualTo(AccountType.SAVINGS);
        assertThat(savedAccount.getAccountNumber()).startsWith("HR");

        assertThat(eventCaptor.getValue().email()).isEqualTo("user@example.com");
        assertThat(eventCaptor.getValue().accountName()).isEqualTo("Sylvester Onah");
        assertThat(auditCaptor.getValue().admin()).isEqualTo("Sylvester Onah");
        assertThat(auditCaptor.getValue().action()).isEqualTo("Account Creation");
    }

    @Test
    void createAccountThrowsWhenUserDoesNotExist() {
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createAccount(new CreateAccountRequest("SAVINGS"), "missing-user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("user not found");

        verifyNoInteractions(accountRepository, publisher, auditLogService);
    }

    @Test
    void createAccountThrowsWhenAccountTypeAlreadyExistsForUser() {
        User user = user();
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(accountRepository.existsByOwner_IdAndAccountTypes("user-123", AccountType.SAVINGS)).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(new CreateAccountRequest("SAVINGS"), "user-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You can only have one SAVINGS account");

        verifyNoInteractions(publisher, auditLogService);
    }

    @Test
    void getAllAccountsReturnsFirstPageContent() {
        Account account = Account.builder().id(1L).accountNumber("HR-test").build();
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(account)));

        assertThat(accountService.getAllAccounts()).containsExactly(account);
    }

    @Test
    void getAllUsersAccountsMapsAccountsToResponses() {
        Account account = Account.builder()
                .id(1L)
                .accountNumber("HR-test")
                .accountTypes(AccountType.CURRENT)
                .owner(user())
                .balance(new BigDecimal("12.50"))
                .currency(CurrencyType.EURO)
                .status(AccountStatus.ACTIVE)
                .build();
        when(accountRepository.findAllByOwner_Id("user-123")).thenReturn(List.of(account));

        List<AccountResponse> responses = accountService.getAllUsersAccounts("user-123");

        assertThat(responses).containsExactly(new AccountResponse(1L, "HR-test", "CURRENT", "Sylvester Onah", new BigDecimal("12.50"), "EURO"));
        verify(accountRepository).findAllByOwner_Id("user-123");
    }

    private User user() {
        return User.builder()
                .id("user-123")
                .firstName("Sylvester")
                .lastName("Onah")
                .username("sylvester")
                .email("user@example.com")
                .build();
    }
}
