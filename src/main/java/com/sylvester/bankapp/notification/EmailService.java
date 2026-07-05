package com.sylvester.bankapp.notification;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String sender;

    @Async
    public void sendEmail(String to, String user, String code) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setFrom(sender);
        mailMessage.setSubject("Email Verification");
        mailMessage.setText("Dear " + user + "!\nYour verification code is: " + code+". \nIt expires in 15 minutes.\n" +
                "Your Bank Team");
        mailSender.send(mailMessage);
    }


    @Async
    public void sendAccountEmail(String to, String firstname, String accountNumber, String owner, String accountType) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setFrom(sender);
        mailMessage.setSubject("Account Creation Success");
        mailMessage.setText("Dear " + firstname + "!\nYou have successfully created a "+accountType+" account and it is ready to be used\n" +
                "Your account details: \n" +
                "Account name: " + owner + "\n" +
                "Account number: "+ accountNumber + "\n" +
                "Account type: "+accountType + "\n"+

                "Your Bank Team");

        mailSender.send(mailMessage);
    }

    @Async
    public void sendCreditEmail(String to, String user, String amount, String senderAccountNumber, String senderName) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setFrom(sender);
        mailMessage.setSubject("Credit Alert!");
        mailMessage.setText("Dear " + user + "!\nYour account has been successfully credited.\n" +
                "Account name of the sender: "+senderName+"\n" +
                "Account number of the sender: "+senderAccountNumber+"\n" +
                "Amount: "+amount+"\n" +
                "Your Bank Team");
        mailSender.send(mailMessage);
    }

    @Async
    public void sendDepositEmail(String to, String user, String amount) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setFrom(sender);
        mailMessage.setSubject("Credit Alert!");
        mailMessage.setText("Dear "+user+"!\n" +
                "Your have successfully deposited "+amount+" into your account");
        mailSender.send(mailMessage);
    }

    @Async
    public void sendWithdrawEmail(String to, String user, String amount) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setFrom(sender);
        mailMessage.setSubject("Debit Alert!");
        mailMessage.setText("Dear "+user+"!\n" +
                "Your have successfully withdrawn "+amount+" from your account");
        mailSender.send(mailMessage);
    }

    @Async
    public void sendReceiptEmail(String toEmail, String user, BigDecimal amount, String recipient, String transactionId, byte[] receipt) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(toEmail);
            helper.setFrom(sender);
            helper.setSubject("Transaction Notification");
            helper.setText("Hi "+user+",\n" +
                    "You have successfully transferred € "+amount+" to "+recipient);
            helper.addAttachment("receipt-"+transactionId+".pdf",
                    new ByteArrayResource(receipt));
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendEmailWithAttachmentBytes(
            String to,
            String user,
            byte[] attachment
    ) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject("STATEMENT OF ACCOUNT");
        helper.setText("Dear "+user+"\n" +
                "Kindly find your requested account statement attached.\n" +
                "Your Bank Team");

        helper.addAttachment(
                "statement.pdf",
                new ByteArrayResource(attachment)
        );

        mailSender.send(message);
    }










}
