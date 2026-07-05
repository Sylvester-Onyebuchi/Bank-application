package com.sylvester.bankapp.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "sender", "bank@example.com");
    }

    @Test
    void sendEmailBuildsVerificationEmail() {
        emailService.sendEmail("user@example.com", "Sylvester", "123456");

        SimpleMailMessage message = captureSimpleMessage();
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getFrom()).isEqualTo("bank@example.com");
        assertThat(message.getSubject()).isEqualTo("Email Verification");
        assertThat(message.getText()).contains("Sylvester", "123456", "expires in 15 minutes");
    }

    @Test
    void sendAccountEmailIncludesCreatedAccountDetails() {
        emailService.sendAccountEmail("user@example.com", "Sylvester", "HR123", "Sylvester Onah", "SAVINGS");

        SimpleMailMessage message = captureSimpleMessage();
        assertThat(message.getSubject()).isEqualTo("Account Creation Success");
        assertThat(message.getText()).contains("HR123", "Sylvester Onah", "SAVINGS");
    }

    @Test
    void transactionAlertEmailsUseExpectedSubjectsAndContent() {
        emailService.sendCreditEmail("user@example.com", "Sylvester", "10.00", "HR1", "Sender Name");
        emailService.sendDepositEmail("user@example.com", "Sylvester", "25.00");
        emailService.sendWithdrawEmail("user@example.com", "Sylvester", "15.00");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.times(3)).send(captor.capture());

        assertThat(captor.getAllValues().get(0).getSubject()).isEqualTo("Credit Alert!");
        assertThat(captor.getAllValues().get(0).getText()).contains("Sender Name", "HR1", "10.00");
        assertThat(captor.getAllValues().get(1).getText()).contains("deposited 25.00");
        assertThat(captor.getAllValues().get(2).getSubject()).isEqualTo("Debit Alert!");
        assertThat(captor.getAllValues().get(2).getText()).contains("withdrawn 15.00");
    }

    @Test
    void sendReceiptEmailCreatesMimeMessageWithReceiptAttachment() throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendReceiptEmail("user@example.com", "Sylvester", new BigDecimal("10.00"), "Recipient", "tx-123", "pdf".getBytes());

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("Transaction Notification");
    }

    @Test
    void sendEmailWithAttachmentBytesCreatesStatementEmail() throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendEmailWithAttachmentBytes("user@example.com", "Sylvester", "statement".getBytes());

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("STATEMENT OF ACCOUNT");
    }

    private SimpleMailMessage captureSimpleMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
