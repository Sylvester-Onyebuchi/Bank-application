package com.sylvester.bankapp.userSecurity.service;


import com.sylvester.bankapp.userSecurity.entity.UserSecurity;
import com.sylvester.bankapp.userSecurity.repository.UserSecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HandleFailedAttempts {

    private final UserSecurityRepository userSecurityRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedAttempts(UserSecurity userSecurity) {
        int attempts = userSecurity.getPinFailedAttempts() + 1;
        userSecurity.setPinFailedAttempts(attempts);
        if (attempts >= 5) {
            userSecurity.setPinLockedUntil(LocalDateTime.now().plusMinutes(10));
        }

        userSecurityRepository.saveAndFlush(userSecurity);
    }
}
