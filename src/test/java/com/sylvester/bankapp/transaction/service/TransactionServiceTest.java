package com.sylvester.bankapp.transaction.service;

import com.sylvester.bankapp.account.entity.Account;
import com.sylvester.bankapp.account.entity.AccountStatus;
import com.sylvester.bankapp.account.entity.IdempotencyKey;
import com.sylvester.bankapp.account.repository.AccountRepository;
import com.sylvester.bankapp.account.repository.IdempotencyRepository;
import com.sylvester.bankapp.exception.NotFoundException;
import com.sylvester.bankapp.rabbitmq.Publisher;
import com.sylvester.bankapp.rabbitmq.dto.DepositEvent;
import com.sylvester.bankapp.rabbitmq.dto.StatementEvent;
import com.sylvester.bankapp.rabbitmq.dto.TransferEvent;
import com.sylvester.bankapp.rabbitmq.dto.WithDrawEvent;
import com.sylvester.bankapp.receipt.BankStatement;
import com.sylvester.bankapp.transaction.dto.DepositAndWithdrawRequest;
import com.sylvester.bankapp.transaction.dto.TransactionDto;
import com.sylvester.bankapp.transaction.dto.TransferRequest;
import com.sylvester.bankapp.transaction.entity.Transaction;
import com.sylvester.bankapp.transaction.entity.TransactionStatus;
import com.sylvester.bankapp.transaction.repository.TransactionRepository;
import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.user.entity.UserStatus;
import com.sylvester.bankapp.user.repository.UserRepository;
import com.sylvester.bankapp.userSecurity.service.SecurityService;
import org.apache.commons.codec.digest.DigestUtils;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Publisher publisher;

    @Mock
    private BankStatement bankStatement;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void depositMoneyCreditsAccountCreatesTransactionAndPublishesEvent() {
        User user = user("user-123", "Sender", "User", "sender@example.com", UserStatus.ACTIVE);
        Account account = account(1L, iban("1234567890"), user, "100.00", AccountStatus.ACTIVE);
        DepositAndWithdrawRequest request = new DepositAndWithdrawRequest(account.getAccountNumber(), new BigDecimal("25.50"));
        when(accountRepository.findByAccountNumberAndOwner_Id(account.getAccountNumber(), "user-123")).thenReturn(Optional.of(account));

        transactionService.depositMoney(request, "user-123");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        ArgumentCaptor<DepositEvent> eventCaptor = ArgumentCaptor.forClass(DepositEvent.class);
        verify(accountRepository).save(account);
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(publisher).publishDeposit(eventCaptor.capture());

        assertThat(account.getBalance()).isEqualByComparingTo("125.50");
        assertThat(transactionCaptor.getValue().getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(eventCaptor.getValue().accountName()).isEqualTo(account.getAccountNumber());
        assertThat(eventCaptor.getValue().amount()).isEqualByComparingTo("25.50");
    }

    @Test
    void withdrawMoneyDebitsAccountCreatesTransactionAndPublishesEvent() {
        User user = user("user-123", "Sender", "User", "sender@example.com", UserStatus.ACTIVE);
        Account account = account(1L, iban("1234567890"), user, "100.00", AccountStatus.ACTIVE);
        DepositAndWithdrawRequest request = new DepositAndWithdrawRequest(account.getAccountNumber(), new BigDecimal("30.00"));
        when(accountRepository.findByAccountNumberAndOwner_Id(account.getAccountNumber(), "user-123")).thenReturn(Optional.of(account));

        transactionService.withDrawMoney(request, "user-123");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        ArgumentCaptor<WithDrawEvent> eventCaptor = ArgumentCaptor.forClass(WithDrawEvent.class);
        verify(accountRepository).save(account);
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(publisher).publishWithdraw(eventCaptor.capture());

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
        assertThat(transactionCaptor.getValue().getTransactionType()).isEqualTo("WITHDRAWAL");
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(eventCaptor.getValue().accountName()).isEqualTo(account.getAccountNumber());
    }

    @Test
    void transferCreatesIdempotencyRecordTransfersFundsVerifiesPinAndPublishesTransferEvent() {
        User sender = user("sender-id", "Sender", "User", "sender@example.com", UserStatus.ACTIVE);
        User recipient = user("recipient-id", "Recipient", "User", "recipient@example.com", UserStatus.ACTIVE);
        Account senderAccount = account(1L, iban("1234567890"), sender, "200.00", AccountStatus.ACTIVE);
        Account recipientAccount = account(2L, iban("9876543210"), recipient, "50.00", AccountStatus.ACTIVE);
        TransferRequest request = new TransferRequest(
                senderAccount.getAccountNumber(),
                recipientAccount.getAccountNumber(),
                recipient.getFullName(),
                new BigDecimal("75.00"),
                "Lunch",
                "1234"
        );

        when(idempotencyRepository.findByKey("request-key")).thenReturn(Optional.empty());
        when(idempotencyRepository.saveAndFlush(any(IdempotencyKey.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById("sender-id")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber(senderAccount.getAccountNumber())).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber(recipientAccount.getAccountNumber())).thenReturn(Optional.of(recipientAccount));

        transactionService.transfer(request, "request-key", "sender-id");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        ArgumentCaptor<IdempotencyKey> idempotencyCaptor = ArgumentCaptor.forClass(IdempotencyKey.class);
        ArgumentCaptor<TransferEvent> eventCaptor = ArgumentCaptor.forClass(TransferEvent.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(idempotencyRepository).save(idempotencyCaptor.capture());
        verify(securityService).verifyTransactionPin("sender-id", "1234");
        verify(publisher).publishTransfer(eventCaptor.capture());

        assertThat(senderAccount.getBalance()).isEqualByComparingTo("125.00");
        assertThat(recipientAccount.getBalance()).isEqualByComparingTo("125.00");
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(idempotencyCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(idempotencyCaptor.getValue().getRequestHash()).isEqualTo(requestHash(request));
        assertThat(eventCaptor.getValue().recipientName()).isEqualTo("Recipient User");
    }

    @Test
    void transferDoesNothingWhenIdempotencyRecordAlreadySucceeded() {
        TransferRequest request = new TransferRequest(iban("1234567890"), iban("9876543210"), "Recipient User", BigDecimal.TEN, "test", "1234");
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .key("request-key")
                .status(TransactionStatus.SUCCESS)
                .requestHash(requestHash(request))
                .build();
        when(idempotencyRepository.findByKey("request-key")).thenReturn(Optional.of(idempotencyKey));

        transactionService.transfer(request, "request-key", "sender-id");

        verifyNoInteractions(userRepository, accountRepository, publisher, transactionRepository, securityService);
    }

    @Test
    void transferMarksIdempotencyRecordFailedWhenBusinessValidationFails() {
        User sender = user("sender-id", "Sender", "User", "sender@example.com", UserStatus.ACTIVE);
        User recipient = user("recipient-id", "Recipient", "User", "recipient@example.com", UserStatus.ACTIVE);
        Account senderAccount = account(1L, iban("1234567890"), sender, "20.00", AccountStatus.ACTIVE);
        Account recipientAccount = account(2L, iban("9876543210"), recipient, "50.00", AccountStatus.ACTIVE);
        TransferRequest request = new TransferRequest(senderAccount.getAccountNumber(), recipientAccount.getAccountNumber(), recipient.getFullName(), new BigDecimal("75.00"), "Lunch", "1234");

        when(idempotencyRepository.findByKey("request-key")).thenReturn(Optional.empty());
        when(idempotencyRepository.saveAndFlush(any(IdempotencyKey.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById("sender-id")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber(senderAccount.getAccountNumber())).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByAccountNumber(recipientAccount.getAccountNumber())).thenReturn(Optional.of(recipientAccount));

        assertThatThrownBy(() -> transactionService.transfer(request, "request-key", "sender-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds.");

        ArgumentCaptor<IdempotencyKey> idempotencyCaptor = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(idempotencyRepository).save(idempotencyCaptor.capture());
        assertThat(idempotencyCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.FAILED);
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(publisher, never()).publishTransfer(any(TransferEvent.class));
    }

    @Test
    void getReceiptReturnsStoredReceiptBytesAndThrowsWhenMissing() {
        byte[] receipt = "pdf".getBytes();
        when(transactionRepository.findByTransactionId("tx-1"))
                .thenReturn(Optional.of(Transaction.builder().transactionId("tx-1").receiptPdf(receipt).build()));
        when(transactionRepository.findByTransactionId("tx-2"))
                .thenReturn(Optional.of(Transaction.builder().transactionId("tx-2").build()));

        assertThat(transactionService.getReceipt("tx-1")).isEqualTo(receipt);
        assertThatThrownBy(() -> transactionService.getReceipt("tx-2"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Receipt not available");
    }

    @Test
    void statementOfAccountGeneratesStatementAndPublishesEmailEvent() throws Exception {
        User user = user("user-123", "Sender", "User", "sender@example.com", UserStatus.ACTIVE);
        byte[] pdf = "statement-pdf".getBytes();
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(bankStatement.generateStatement("HR1", "2026-01-01", "2026-01-31", "user-123", user.getUsername(), user.getAddress()))
                .thenReturn(pdf);

        transactionService.statementOfAccount("HR1", "2026-01-01", "2026-01-31", "user-123");

        ArgumentCaptor<StatementEvent> eventCaptor = ArgumentCaptor.forClass(StatementEvent.class);
        verify(publisher).publishStatement(eventCaptor.capture());
        assertThat(eventCaptor.getValue().email()).isEqualTo("sender@example.com");
        assertThat(eventCaptor.getValue().username()).isEqualTo("Sender User");
        assertThat(eventCaptor.getValue().pdf()).isEqualTo(pdf);
    }

    @Test
    void getLastTenTransactionsMapsDebitAndCreditDescriptions() {
        User owner = user("user-123", "Owner", "User", "owner@example.com", UserStatus.ACTIVE);
        User other = user("other-id", "Other", "User", "other@example.com", UserStatus.ACTIVE);
        Account selected = account(1L, "HR1", owner, "100.00", AccountStatus.ACTIVE);
        Account otherAccount = account(2L, "HR2", other, "50.00", AccountStatus.ACTIVE);
        Transaction outgoing = Transaction.builder()
                .amount(BigDecimal.TEN)
                .transactionType("TRANSFER")
                .senderAccount(selected)
                .recipientAccount(otherAccount)
                .status(TransactionStatus.SUCCESS)
                .build();
        Transaction deposit = Transaction.builder()
                .amount(new BigDecimal("15.00"))
                .transactionType("DEPOSIT")
                .senderAccount(selected)
                .status(TransactionStatus.SUCCESS)
                .build();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(selected));
        when(transactionRepository.findLastTenTransactionForAccount(any(Long.class), any())).thenReturn(List.of(outgoing, deposit));

        List<TransactionDto> transactions = transactionService.getLastTenTransactions("user-123", 1L);

        assertThat(transactions).extracting(TransactionDto::direction).containsExactly("DEBIT", "DEBIT");
        assertThat(transactions).extracting(TransactionDto::description).containsExactly("Transfer to Other User", "ATM Deposit");
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

    private User user(String id, String firstName, String lastName, String email, UserStatus status) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .username(firstName.toLowerCase())
                .email(email)
                .address("123 Main Street")
                .status(status)
                .build();
    }

    private String iban(String accountNumber) {
        return new Iban.Builder()
                .countryCode(CountryCode.HR)
                .bankCode("2360000")
                .accountNumber(accountNumber)
                .build()
                .toString();
    }

    private String requestHash(TransferRequest request) {
        return DigestUtils.sha256Hex(String.join("|",
                request.accountNumber(),
                request.recipientAccountNumber(),
                request.recipientName(),
                request.amount().toPlainString(),
                request.narration() == null ? "" : request.narration()
        ));
    }
}
