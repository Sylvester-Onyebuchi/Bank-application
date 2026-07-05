package com.sylvester.bankapp.userSecurity.service;

import com.sylvester.bankapp.userSecurity.entity.UserSecurity;
import com.sylvester.bankapp.userSecurity.repository.UserSecurityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HandleFailedAttemptsTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    @InjectMocks
    private HandleFailedAttempts handleFailedAttempts;

    @Test
    void handleFailedAttemptsIncrementsAttemptsAndLocksAfterFiveFailures() {
        UserSecurity security = UserSecurity.builder().pinFailedAttempts(4).build();

        handleFailedAttempts.handleFailedAttempts(security);

        assertThat(security.getPinFailedAttempts()).isEqualTo(5);
        assertThat(security.getPinLockedUntil()).isNotNull();
        verify(userSecurityRepository).saveAndFlush(security);
    }
}
