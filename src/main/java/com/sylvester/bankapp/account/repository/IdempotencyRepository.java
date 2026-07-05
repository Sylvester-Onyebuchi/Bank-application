package com.sylvester.bankapp.account.repository;

import com.sylvester.bankapp.account.entity.IdempotencyKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, String> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM IdempotencyKey i
        WHERE i.key = :key
    """)
    Optional<IdempotencyKey> findByKey( @Param("key") String key);

}
