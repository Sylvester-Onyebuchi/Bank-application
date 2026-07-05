package com.sylvester.bankapp.userSecurity.service;


import com.sylvester.bankapp.exception.AlreadyExistException;
import com.sylvester.bankapp.exception.NotFoundException;
import com.sylvester.bankapp.user.repository.UserRepository;
import com.sylvester.bankapp.userSecurity.entity.UserSecurity;
import com.sylvester.bankapp.userSecurity.repository.UserSecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

    private final UserSecurityRepository userSecurityRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final HandleFailedAttempts handleFailedAttempts;

    @Transactional
    public void createPin(String pin, String userId) {
        var user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User not found")
        );
        if (userSecurityRepository.existsByUser(user)) {
            throw new AlreadyExistException("You have already created transaction pin");
        }

        UserSecurity userSecurity = UserSecurity.builder()
                .user(user)
                .transactionPin(passwordEncoder.encode(pin))
                .build();
        userSecurityRepository.save(userSecurity);
        log.info("Transaction pin created successfully");
    }

    @Transactional
    public void changePin(String newPin, String userId) {

        UserSecurity userSecurity = userSecurityRepository.findByUser_Id(userId).orElseThrow(
                () -> new NotFoundException("User not found")
        );
        userSecurity.setTransactionPin(passwordEncoder.encode(newPin));
       userSecurityRepository.save(userSecurity);
       log.info("Transaction pin changed successfully");
    }


    @Transactional
    public void verifyTransactionPin(String userId, String rawPin) {

        UserSecurity security = userSecurityRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Transaction PIN not found"));

        if (security.getPinLockedUntil() != null &&
                security.getPinLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Transaction PIN is temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(rawPin, security.getTransactionPin())) {
            handleFailedAttempts.handleFailedAttempts(security);
            throw new RuntimeException("Invalid transaction PIN.");
        }

        security.setPinFailedAttempts(0);
        security.setPinLockedUntil(null);
    }




}
