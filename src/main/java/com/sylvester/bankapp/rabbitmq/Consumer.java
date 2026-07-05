package com.sylvester.bankapp.rabbitmq;


import com.sylvester.bankapp.exception.NotFoundException;
import com.sylvester.bankapp.notification.EmailService;
import com.sylvester.bankapp.rabbitmq.dto.*;
import com.sylvester.bankapp.receipt.GenerateReceipt;
import com.sylvester.bankapp.receipt.ReceiptDto;
import com.sylvester.bankapp.transaction.entity.Transaction;
import com.sylvester.bankapp.transaction.entity.TransactionStatus;
import com.sylvester.bankapp.transaction.repository.TransactionRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class Consumer {

    private final GenerateReceipt generateReceipt;
    private final EmailService emailService;
    private final TransactionRepository transactionRepository;

    @RabbitListener(queues = "${transfer.queue}")
    public void receiveTransaction(TransferEvent event) {

        log.info("Received transfer event: {}", event);

        ReceiptDto receiptDto = ReceiptDto.builder()
                .transactionId(event.transactionId())
                .senderAccountNumber(event.senderAccountNumber())
                .senderName(event.senderName())
                .recipientAccountNumber(event.recipientAccountNumber())
                .recipientName(event.recipientName())
                .amount(event.amount())
                .transactionType("TRANSFER")
                .transactionReference(event.transactionReference())
                .status(TransactionStatus.SUCCESS)
                .createdDate(LocalDateTime.now())
                .build();

        byte[] receipt = generateReceipt.generateReceipt(receiptDto);


        Transaction transaction = transactionRepository
                .findByTransactionId(event.transactionId())
                .orElseThrow(() ->  new NotFoundException("transaction not found"));

        transaction.setReceiptPdf(receipt);
        transactionRepository.save(transaction);

        emailService.sendCreditEmail(
                event.recipientEmail(),
                event.recipientName(),
                event.amount().toString(),
                event.senderAccountNumber(),
                event.senderName()
        );

        emailService.sendReceiptEmail(
                event.senderEmail(),
                event.senderName(),
                event.amount(),
                event.recipientName(),
                event.transactionId(),
                receipt
        );
        log.info("Transaction {} has been sent successfully", event.senderName());

    }

    @RabbitListener(queues = "${withdraw.queue}")
    public void receiveWithdraw(WithDrawEvent event) {

        emailService.sendWithdrawEmail(event.email(), event.username(),  String.valueOf(event.amount()));
    }

    @RabbitListener(queues = "${deposit.queue}")
    public void receiveDeposit(DepositEvent event) {

        emailService.sendDepositEmail(event.email(), event.username(),  String.valueOf(event.amount()));
    }

    @RabbitListener(queues = "${statement.queue}")
    public void receiveStatement(StatementEvent event) {

        log.info("Received statement {}", event);

        try {
            emailService.sendEmailWithAttachmentBytes(event.email(), event.username(),  event.pdf());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @RabbitListener(queues = "${account.queue}")
    public void receiveAccount(AccountEvent event) {
        emailService.sendAccountEmail(event.email(), event.firstname(), event.accountNumber(), event.accountName(), event.accountType());
    }



}
