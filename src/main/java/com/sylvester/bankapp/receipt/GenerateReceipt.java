package com.sylvester.bankapp.receipt;


import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class GenerateReceipt {

    public byte[] generateReceipt(ReceiptDto receiptDto) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter pdfWriter = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDocument);
            Paragraph title = new Paragraph("Transaction Receipt")
                    .setBold()
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));
            Paragraph amount = new Paragraph("€ "+receiptDto.getAmount())
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold();
            document.add(amount);
            document.add(new Paragraph("\n"));

            Table table = new Table(UnitValue.createPercentArray(2))
                    .useAllAvailableWidth();
            table.setBorder(new SolidBorder(ColorConstants.GRAY,1));
            addRow(table, "Transaction Type", receiptDto.getTransactionType());
            addRow(table, "Transaction Status", String.valueOf(receiptDto.getStatus()));
            addRow(table, "Sender Name", receiptDto.getSenderName());
            addRow(table, "Sender Account", receiptDto.getSenderAccountNumber());
            addRow(table, "Bank name", "M-ZABA");
            addRow(table, "Beneficiary", receiptDto.getRecipientName() + " | ".toUpperCase() + receiptDto.getRecipientAccountNumber());
            addRow(table, "Transaction Date", format(receiptDto.getCreatedDate()));
            addRow(table, "Transaction Reference", receiptDto.getTransactionReference());
            addRow(table, "Transaction Id", receiptDto.getTransactionId());

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate receipt.", e);
        }
    }

    private void addRow(Table table, String key, String value) {
        table.addCell(new Cell().add(new Paragraph(key).setBold()));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private  String format(LocalDateTime dateTime) {

        String dayOfWeek = dateTime.format(
                DateTimeFormatter.ofPattern("EEEE"));

        String month = dateTime.format(
                DateTimeFormatter.ofPattern("MMMM"));

        int day = dateTime.getDayOfMonth();

        String suffix = getDaySuffix(day);

        String year = dateTime.format(
                DateTimeFormatter.ofPattern("yyyy"));

        String time = dateTime.format(
                DateTimeFormatter.ofPattern("hh:mm a"));

        return String.format(
                "%s, %s %d%s, %s | %s",
                dayOfWeek,
                month,
                day,
                suffix,
                year,
                time
        );
    }

    private static String getDaySuffix(int day) {

        if (day >= 11 && day <= 13) return "th";

        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

}
