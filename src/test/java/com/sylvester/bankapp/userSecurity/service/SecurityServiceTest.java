package com.sylvester.bankapp.userSecurity.service;

import com.sylvester.bankapp.exception.AlreadyExistException;
import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.user.repository.UserRepository;
import com.sylvester.bankapp.userSecurity.entity.UserSecurity;
import com.sylvester.bankapp.userSecurity.repository.UserSecurityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HandleFailedAttempts handleFailedAttempts;

    @InjectMocks
    private SecurityService securityService;

    @Test
    void createPinEncodesAndSavesPin() {
        User user = User.builder().id("user-123").build();
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(userSecurityRepository.existsByUser(user)).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("encoded");

        securityService.createPin("1234", "user-123");

        ArgumentCaptor<UserSecurity> captor = ArgumentCaptor.forClass(UserSecurity.class);
        verify(userSecurityRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getTransactionPin()).isEqualTo("encoded");
    }

    @Test
    void createPinRejectsExistingPin() {
        User user = User.builder().id("user-123").build();
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(userSecurityRepository.existsByUser(user)).thenReturn(true);

        assertThatThrownBy(() -> securityService.createPin("1234", "user-123"))
                .isInstanceOf(AlreadyExistException.class)
                .hasMessage("You have already created transaction pin");
    }

    @Test
    void changePinUpdatesStoredPin() {
        UserSecurity security = UserSecurity.builder().transactionPin("old").build();
        when(userSecurityRepository.findByUser_Id("user-123")).thenReturn(Optional.of(security));
        when(passwordEncoder.encode("4321")).thenReturn("new-encoded");

        securityService.changePin("4321", "user-123");

        assertThat(security.getTransactionPin()).isEqualTo("new-encoded");
        verify(userSecurityRepository).save(security);
    }

    @Test
    void verifyTransactionPinResetsAttemptsOnSuccessAndDelegatesFailureHandlingOnMismatch() {
        UserSecurity security = UserSecurity.builder().transactionPin("encoded").pinFailedAttempts(3).build();
        when(userSecurityRepository.findByUser_Id("user-123")).thenReturn(Optional.of(security));
        when(passwordEncoder.matches("1234", "encoded")).thenReturn(true);

        securityService.verifyTransactionPin("user-123", "1234");

        assertThat(security.getPinFailedAttempts()).isZero();
        assertThat(security.getPinLockedUntil()).isNull();

        when(passwordEncoder.matches("9999", "encoded")).thenReturn(false);
        assertThatThrownBy(() -> securityService.verifyTransactionPin("user-123", "9999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid transaction PIN.");
        verify(handleFailedAttempts).handleFailedAttempts(security);
    }

    @Test
    void verifyTransactionPinRejectsTemporarilyLockedPin() {
        UserSecurity security = UserSecurity.builder()
                .transactionPin("encoded")
                .pinLockedUntil(LocalDateTime.now().plusMinutes(5))
                .build();
        when(userSecurityRepository.findByUser_Id("user-123")).thenReturn(Optional.of(security));

        assertThatThrownBy(() -> securityService.verifyTransactionPin("user-123", "1234"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transaction PIN is temporarily locked. Try again later.");
    }
}
