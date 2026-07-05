package com.sylvester.bankapp.receipt;


import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.sylvester.bankapp.account.entity.Account;
import com.sylvester.bankapp.account.repository.AccountRepository;
import com.sylvester.bankapp.transaction.entity.Transaction;
import com.sylvester.bankapp.transaction.repository.TransactionRepository;
import com.sylvester.bankapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankStatement {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;


    public byte[] generateStatement(
            String accountNumber,
            String startDate,
            String endDate,
            String userId,
            String username,
            String addres
    ) throws Exception {

        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);



        Account account = accountRepository.findByOwner_IdAndAccountNumber(userId, accountNumber).orElseThrow(
                () -> new Exception("Account not found")
        );


        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> t.getSenderAccount().getAccountNumber().equals(account.getAccountNumber())
                        || t.getRecipientAccount().getAccountNumber().equals(account.getAccountNumber())
                )
                .filter(transaction -> !transaction.getCreatedDate().isBefore(start) &&
                        !transaction.getCreatedDate().isAfter(end))
                .toList();

       ByteArrayOutputStream base = new ByteArrayOutputStream();
        Rectangle statementSize = new Rectangle(PageSize.A4);
        Document document = new Document(statementSize);
        PdfWriter.getInstance(document, base);

        document.open();

        Font whiteFont = new Font(
                Font.FontFamily.HELVETICA,
                12,
                Font.BOLD,
                BaseColor.WHITE
        );
        PdfPTable table = new PdfPTable(1);
        PdfPCell bankName = new PdfPCell(new Phrase("Zagrebacka Banka(M-Zaba)", whiteFont));
        bankName.setBorder(0);
        table.setSpacingAfter(20f);
        bankName.setBackgroundColor(BaseColor.RED);
        bankName.setPadding(20f);

        PdfPCell bankAddress = new PdfPCell(new Phrase("Miramarska Cesta 23, 10000, Zagreb"));
        bankAddress.setBorder(0);

        table.addCell(bankName);
        table.addCell(bankAddress);

        PdfPTable statementInfo = new PdfPTable(2);
        PdfPCell firstDate = new PdfPCell(new Phrase("Start Date: "+startDate));
        firstDate.setBorder(0);

        PdfPCell statement = new PdfPCell(new Phrase("STATEMENT OF ACCOUNT"));
        statement.setBorder(0);

        PdfPCell lastDate = new PdfPCell(new Phrase("End Date: "+endDate));
        lastDate.setBorder(0);

        PdfPCell name = new PdfPCell(new Phrase("Customer Name: "+username));
        name.setBorder(0);

        PdfPCell space = new PdfPCell();
        space.setBorder(0);

        PdfPCell address = new PdfPCell(new Phrase("Customer Address: "+addres));
        address.setBorder(0);


        PdfPTable transactionTable = new PdfPTable(4);
        PdfPCell date = new PdfPCell(new Phrase("DATE", whiteFont));
        date.setBorder(0);
        date.setBackgroundColor(BaseColor.RED);


        PdfPCell type = new PdfPCell(new Phrase("TRANSACTION TYPE", whiteFont));
        type.setBorder(0);
        type.setBackgroundColor(BaseColor.RED);

        PdfPCell transactionAmount = new PdfPCell(new Phrase("TRANSACTION AMOUNT", whiteFont));
        transactionAmount.setBorder(0);
        transactionAmount.setBackgroundColor(BaseColor.RED);


        PdfPCell status = new PdfPCell(new Phrase("STATUS", whiteFont));
        status.setBorder(0);
        status.setBackgroundColor(BaseColor.RED);

        transactionTable.addCell(date);
        transactionTable.addCell(type);
        transactionTable.addCell(transactionAmount);
        transactionTable.addCell(status);
        transactionTable.setSpacingBefore(20f);


        transactions.forEach(transaction -> {
            transactionTable.addCell(new Phrase(transaction.getCreatedDate().toString()));
            transactionTable.addCell(new Phrase(transaction.getTransactionType()));
            transactionTable.addCell(new Phrase(transaction.getAmount().toString()));
            transactionTable.addCell(new Phrase(transaction.getStatus().toString()));
        });

        statementInfo.addCell(firstDate);
        statementInfo.addCell(statement);
        statementInfo.addCell(lastDate);
        statementInfo.addCell(name);
        statementInfo.addCell(space);
        statementInfo.addCell(address);

        document.add(table);
        document.add(statementInfo);
        document.add(transactionTable);
        document.close();

        return base.toByteArray();

    }
}
