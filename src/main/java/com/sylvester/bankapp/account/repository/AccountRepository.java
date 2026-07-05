package com.sylvester.bankapp.account.repository;

import com.sylvester.bankapp.account.entity.Account;
import com.sylvester.bankapp.account.entity.AccountType;
import com.sylvester.bankapp.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByOwnerAndAccountNumber(User owner, String accountNumber);

    Optional<Account> findByOwner_IdAndAccountNumber(String ownerId, String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT a
    FROM Account a
    WHERE a.accountNumber = :accountNumber
    """)
    Optional<Account> findByAccountNumber( @Param("accountNumber") String accountNumber);


    boolean existsByOwner_IdAndAccountTypes(String ownerId, AccountType accountTypes);

    List<Account> findAllByAccountNumber(String accountNumber);

    boolean existsByOwner_Id(String ownerId);

    List<Account> findAllByOwner_Id(String ownerId);

    List<Account> findAllByOwner_Email(String ownerEmail);

    Optional<Account> findByAccountNumberAndOwner_Id(String accountNumber, String ownerId);



}
