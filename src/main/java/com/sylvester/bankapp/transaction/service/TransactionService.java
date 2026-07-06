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
import com.sylvester.bankapp.transaction.dto.TransactionDto;
import com.sylvester.bankapp.transaction.dto.TransferRequest;
import com.sylvester.bankapp.transaction.dto.DepositAndWithdrawRequest;
import com.sylvester.bankapp.transaction.entity.Transaction;
import com.sylvester.bankapp.transaction.entity.TransactionStatus;
import com.sylvester.bankapp.transaction.repository.TransactionRepository;
import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.user.entity.UserStatus;
import com.sylvester.bankapp.user.repository.UserRepository;
import com.sylvester.bankapp.userSecurity.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.iban4j.IbanUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final Publisher publisher;
    private final BankStatement bankStatement;
    private final AccountRepository accountRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final SecurityService securityService;


    @Transactional
    public void transfer(TransferRequest request, String idempotencyKey, String userId) {

        String requestHash = generateRequestHash(request);

        Optional<IdempotencyKey> existing =
                idempotencyRepository.findByKey(idempotencyKey);

        if (existing.isPresent()) {

            IdempotencyKey record = existing.get();

            if (!record.getRequestHash().equals(requestHash)) {
                throw new RuntimeException("Idempotency key reused with different request");
            }

            if (record.getStatus() == TransactionStatus.SUCCESS) {
                log.info("Duplicate request ignored. Transaction already completed.");
                return;
            }

            if (record.getStatus() == TransactionStatus.PENDING) {
                throw new RuntimeException("Request already processing.");
            }

            if (record.getStatus() == TransactionStatus.FAILED) {
                throw new RuntimeException("Previous request failed. Use a new idempotency key.");
            }
        }

        IdempotencyKey record = IdempotencyKey.builder()
                .key(idempotencyKey)
                .requestHash(requestHash)
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        idempotencyRepository.saveAndFlush(record);

        try {

            performTransfer(request, userId, record);

        } catch (Exception ex) {

            record.setStatus(TransactionStatus.FAILED);
            idempotencyRepository.save(record);

            throw ex;
        }
    }

    private void performTransfer(TransferRequest request, String userId, IdempotencyKey record) {

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Account senderAccount = accountRepository
                .findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        Account recipientAccount = accountRepository
                .findByAccountNumber(request.recipientAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        User recipientUser = recipientAccount.getOwner();

        if (sender.getStatus().equals(UserStatus.LOCKED)) {
            throw new LockedException("Sender is currently locked");
        }

        if (recipientUser.getStatus().equals(UserStatus.LOCKED)) {
            throw new LockedException("Recipient is currently locked");
        }

        if (!senderAccount.getOwner().getId().equals(userId)) {
            throw new RuntimeException("This account does not belong to you.");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid transfer amount");
        }

        if (!validateAccountNumber(request.recipientAccountNumber())) {
            throw new RuntimeException("Invalid account number");
        }

        if (senderAccount.getId().equals(recipientAccount.getId())) {
            throw new RuntimeException("You cannot transfer to the same account.");
        }

        if (!recipientUser.getFullName().equals(request.recipientName())) {
            throw new RuntimeException("Recipient username does not match.");
        }

        if (senderAccount.getStatus().equals(AccountStatus.LOCKED)) {
            throw new RuntimeException("Sender account is inactive or locked.");
        }

        if (recipientAccount.getStatus().equals(AccountStatus.LOCKED)) {
            throw new RuntimeException("Recipient account is inactive or locked.");
        }

        if (senderAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new RuntimeException("Insufficient funds.");
        }

        securityService.verifyTransactionPin(userId, request.pin());

        senderAccount.setBalance(
                senderAccount.getBalance().subtract(request.amount())
        );

        recipientAccount.setBalance(
                recipientAccount.getBalance().add(request.amount())
        );

        String transactionId = UUID.randomUUID().toString();
        String reference = transactionReference();

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .amount(request.amount())
                .senderAccount(senderAccount)
                .recipientAccount(recipientAccount)
                .createdDate(LocalDate.now())
                .transactionType("TRANSFER")
                .status(TransactionStatus.SUCCESS)
                .transactionReference(reference)
                .narration(request.narration())
                .build();

        transactionRepository.save(transaction);

        record.setStatus(TransactionStatus.SUCCESS);
        record.setTransactionId(transactionId);
        record.setCreatedAt(LocalDateTime.now());

        idempotencyRepository.save(record);

        TransferEvent event = new TransferEvent(
                transactionId,
                senderAccount.getAccountNumber(),
                sender.getFullName(),
                sender.getEmail(),
                recipientAccount.getAccountNumber(),
                recipientUser.getFullName(),
                recipientUser.getEmail(),
                request.amount(),
                reference,
                request.narration()
        );

        publisher.publishTransfer(event);
        log.info("Transfer event published");
    }

    @Transactional
    public void withDrawMoney(DepositAndWithdrawRequest request, String userId) {

        Account accountOwner = accountRepository.findByAccountNumberAndOwner_Id(request.accountNumber(), userId).orElseThrow(
                () -> new NotFoundException("Account not found")
        );

        if (accountOwner.getStatus().equals(AccountStatus.LOCKED)) {
            throw new RuntimeException("Account is locked temporarily");
        }

        if (validateAccountNumber(request.accountNumber())) {
            if (accountOwner.getBalance().compareTo(request.amount()) < 0){
                throw new RuntimeException("Insufficient funds");
            }
            accountOwner.setBalance(accountOwner.getBalance().subtract(request.amount()));
            accountRepository.save(accountOwner);
            log.info("Your account has been debited with {}", request.amount());
            Transaction transaction = Transaction.builder()
                    .amount(request.amount())
                    .transactionId(UUID.randomUUID().toString())
                    .senderAccount(accountOwner)
                    .recipientAccount(null)
                    .createdDate(LocalDate.now())
                    .transactionType("WITHDRAWAL")
                    .status(TransactionStatus.SUCCESS)
                    .transactionReference(transactionReference())
                    .build();
            transactionRepository.save(transaction);
            WithDrawEvent event = new WithDrawEvent(
                    request.accountNumber(),
                   accountOwner.getOwner().getFullName(),
                    accountOwner.getOwner().getEmail(),
                    request.amount()

            );

            publisher.publishWithdraw(event);
            log.info("Withdraw event published");

        }else  {
            throw new RuntimeException("Invalid account number");
        }

    }


    @Transactional
    public void depositMoney(DepositAndWithdrawRequest request, String userId) {

        Account accountOwner = accountRepository.findByAccountNumberAndOwner_Id(request.accountNumber(), userId).orElseThrow(
                () -> new NotFoundException("Account not found")
        );

        if (accountOwner.getStatus().equals(AccountStatus.LOCKED)) {
            throw new RuntimeException("Account is locked temporarily");
        }

        if (validateAccountNumber(request.accountNumber())) {
            accountOwner.setBalance(accountOwner.getBalance().add(request.amount()));
            accountRepository.save(accountOwner);
            log.info("Your account has been credited with {}", request.amount());
            Transaction transaction = Transaction.builder()
                    .amount(request.amount())
                    .transactionId(UUID.randomUUID().toString())
                    .senderAccount(null)
                    .recipientAccount(accountOwner)
                    .createdDate(LocalDate.now())
                    .transactionType("DEPOSIT")
                    .status(TransactionStatus.SUCCESS)
                    .transactionReference(transactionReference())
                    .build();
            transactionRepository.save(transaction);
            DepositEvent event = new DepositEvent(
                    request.accountNumber(),
                    accountOwner.getOwner().getFullName(),
                    accountOwner.getOwner().getEmail(),
                    request.amount()

            );
            publisher.publishDeposit(event);
            log.info("Deposit event published");
        }else   {
            throw new RuntimeException("Invalid account number");
        }

    }

    @Transactional(readOnly = true)
    public byte[] getReceipt(String transactionId){
        Transaction tx = transactionRepository.findByTransactionId(transactionId).orElseThrow(
                () -> new NotFoundException("Transaction not found")
        );
        if (tx.getReceiptPdf() == null){
            throw new NotFoundException("Receipt not available");
        }
        return tx.getReceiptPdf();
    }

    @Transactional(readOnly = true)
    public void statementOfAccount(String accountNumber, String startDate, String endDate, String userId) throws Exception {

        var user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User not found")
        );


        byte[] pdfBytes = bankStatement.generateStatement(accountNumber, startDate, endDate,userId, user.getFullName(),user.getAddress());

        StatementEvent statementEvent = new StatementEvent(
                user.getEmail(),
               user.getFullName(),
                pdfBytes
        );
        log.info("Sending statement to email {}", statementEvent);

        publisher.publishStatement(statementEvent);
        log.info("Statement event published");

    }


    private boolean validateAccountNumber(String accountNumber) {
        try {
            IbanUtil.validate(accountNumber);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    @Transactional(readOnly = true)
    public List<TransactionDto> getLastTenTransactions(String userId, Long accountId) {
        Account account = accountRepository.findById(accountId).orElseThrow(
                () -> new NotFoundException("Account not found")
        );

        if (!account.getOwner().getId().equals(userId)) {
            throw new RuntimeException("You are not the owner of this account");
        }

        return transactionRepository.findLastTenTransactionForAccount(accountId, PageRequest.of(0,10))
                .stream()
                .map(transaction -> mapToResponse(transaction,accountId))
                .toList();

    }

    private TransactionDto mapToResponse(Transaction transaction, Long selectedAccountId) {

        boolean moneyGoingOut =
                transaction.getSenderAccount() != null &&
                        transaction.getSenderAccount().getId().equals(selectedAccountId);

        return new TransactionDto(
                transaction.getTransactionId(),
                transaction.getAmount(),
                transaction.getTransactionType(),
                moneyGoingOut ? "DEBIT" : "CREDIT",
                buildDescription(transaction, selectedAccountId),
                transaction.getCreatedDate(),
                transaction.getStatus()
        );
    }

    private String buildDescription(Transaction transaction, Long selectedAccountId) {

        if (Objects.equals(transaction.getTransactionType(), "DEPOSIT")) {
            return "ATM Deposit";
        }

        if (Objects.equals(transaction.getTransactionType(), "WITHDRAWAL")) {
            return "ATM Withdrawal";
        }

        if (Objects.equals(transaction.getTransactionType(), "TRANSFER")) {
            if (transaction.getSenderAccount().getId().equals(selectedAccountId)) {
                return "Transfer to " + transaction.getRecipientAccount().getOwner().getFullName();
            }else {
                return "Transfer from " + transaction.getSenderAccount().getOwner().getFullName();
            }


        }

        return "Transaction";
    }
    private String transactionReference() {
        SecureRandom random = new SecureRandom();
        String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return "Zaba-"+date+sb;
    }

    private String generateRequestHash(TransferRequest request) {
        String rawData = String.join("|",
                request.accountNumber(),
                request.recipientAccountNumber(),
                request.recipientName(),
                request.amount().toPlainString(),
                request.narration() == null ? "" : request.narration()
        );

        return DigestUtils.sha256Hex(rawData);
    }


}
