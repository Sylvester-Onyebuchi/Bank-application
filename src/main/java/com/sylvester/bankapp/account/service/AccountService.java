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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final Publisher publisher;


    @Transactional
    public void createAccount(CreateAccountRequest request, String userId){


        User user = userRepository.findById(userId).orElseThrow(
                () -> new UsernameNotFoundException("user not found")
        );
        if (accountRepository.existsByOwner_IdAndAccountTypes(user.getId(), AccountType.valueOf(request.accountType()))){

            throw new RuntimeException("You can only have one "+request.accountType()+" account");
        }

        SecureRandom random = new SecureRandom();
        long min = 1_000_000_000L;
        long max = 9_999_999_999L;
        long number = min + (Math.abs(random.nextLong()) % (max - min + 1));
        String accountNumber = Long.toString(number);
        String iban = new Iban.Builder()
                .countryCode(CountryCode.HR)
                .bankCode("2360000")
                .accountNumber(accountNumber).build().toString();

        Account account = Account.builder()
                .accountNumber(iban)
                .balance(BigDecimal.ZERO)
                .currency(CurrencyType.EURO)
                .owner(user)
                .status(AccountStatus.ACTIVE)
                .createdDate(Instant.now())
                .build();
        try {
            account.setAccountTypes(AccountType.valueOf(request.accountType()));
        }catch (IllegalArgumentException e){
            throw new RuntimeException("invalid accountType");
        }

        accountRepository.save(account);

        AccountEvent event = new AccountEvent(user.getEmail(), user.getFirstName(), account.getAccountNumber(), account.getOwner().getFullName(), account.getAccountTypes().toString());
        publisher.publishAccount(event);

        SaveAudit saveAudit = new SaveAudit(
                account.getOwner().getFullName(), "Account Creation", account.getAccountNumber(), "An account type of "+account.getAccountTypes()+" has been created"
        );
        auditLogService.saveLog(saveAudit);


    }


    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public List<Account> getAllAccounts(){
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdDate"));
       return accountRepository.findAll(pageRequest).getContent();
    }



    @Transactional
    public List<AccountResponse> getAllUsersAccounts(String userId){

        return accountRepository.findAllByOwner_Id(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    private AccountResponse mapToResponse(Account account) {

        return new AccountResponse(account.getId(),account.getAccountNumber(),
        account.getAccountTypes().toString(), account.getOwner().getFullName(), account.getBalance(), account.getCurrency().toString());

    }

}
