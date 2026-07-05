package com.sylvester.bankapp.transaction.repository;

import com.sylvester.bankapp.transaction.dto.TransactionDto;
import com.sylvester.bankapp.transaction.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);


    @Query("""
    SELECT t FROM Transaction t
    WHERE t.senderAccount.id = :accountId
    OR t.recipientAccount.id = :accountId
    ORDER BY t.createdDate DESC
""")
    List<Transaction> findLastTenTransactionForAccount(@Param("accountId") Long accountId, Pageable pageable);
}
