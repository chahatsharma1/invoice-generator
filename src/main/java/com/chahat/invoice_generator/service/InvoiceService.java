package com.chahat.invoice_generator.service;

import com.chahat.invoice_generator.record.Dealer;
import com.chahat.invoice_generator.record.Vehicle;
import com.chahat.invoice_generator.request.InvoiceRequest;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvoiceService {

    // Hardcoded data lists, matching the controllers
    private static final List<Dealer> DEALERS = List.of(
            new Dealer(1L, "Maruti Suzuki Arena", "Connaught Place, New Delhi", "011-23456789"),
            new Dealer(2L, "Hyundai Motors India", "Anna Salai, Chennai", "044-22334455"),
            new Dealer(3L, "Tata Motors Showroom", "FC Road, Pune", "020-33445566")
    );

    private static final List<Vehicle> VEHICLES = List.of(
            new Vehicle(101L, "Maruti", "Swift Dzire", 750000.00, 1L),
            new Vehicle(102L, "Hyundai", "Creta", 1200000.00, 2L),
            new Vehicle(103L, "Tata", "Nexon EV", 1500000.00, 3L),
            new Vehicle(104L, "Maruti", "Baleno", 850000.00, 1L)
    );

    public byte[] generateInvoice(InvoiceRequest request) throws Exception {
        Dealer dealer = DEALERS.stream()
                .filter(d -> d.id().equals(request.getDealerId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Dealer not found!"));

        Vehicle vehicle = VEHICLES.stream()
                .filter(v -> v.id().equals(request.getVehicleId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Vehicle not found!"));

        String invoiceNumber = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        BigDecimal price = BigDecimal.valueOf(vehicle.price());
        BigDecimal taxRate = new BigDecimal("0.10");
        BigDecimal tax = price.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = price.add(tax);

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType0Font regularFont;
            PDType0Font boldFont;

            try (InputStream fontStream = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf")) {
                if (fontStream == null) throw new IOException("Could not find font: DejaVuSans.ttf");
                regularFont = PDType0Font.load(document, fontStream);
            }
            try (InputStream fontStream = getClass().getResourceAsStream("/fonts/DejaVuSans-Bold.ttf")) {
                if (fontStream == null) throw new IOException("Could not find font: DejaVuSans-Bold.ttf");
                boldFont = PDType0Font.load(document, fontStream);
            }

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawHeader(content, boldFont, dealer);
                drawInvoiceInfo(content, regularFont, invoiceNumber, timestamp);
                drawCustomerAndDealerInfo(content, regularFont, boldFont, request.getCustomerName(), dealer);
                drawInvoiceTable(content, regularFont, boldFont, vehicle, price, tax, total, currencyFormatter);
                drawFooter(content, regularFont);
            }

            drawQrCode(document, page, invoiceNumber);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private void drawHeader(PDPageContentStream content, PDType0Font boldFont, Dealer dealer) throws IOException {
        content.beginText();
        content.setFont(boldFont, 24);
        content.newLineAtOffset(50, 800);
        content.showText("INVOICE");
        content.endText();

        content.beginText();
        content.setFont(boldFont, 12);
        content.newLineAtOffset(50, 780);
        content.showText(dealer.name());
        content.endText();
    }

    private void drawInvoiceInfo(PDPageContentStream content, PDType0Font font, String invoiceNumber, String timestamp) throws IOException {
        content.setFont(font, 10);
        drawTextRightAligned(content, font, 10, "Invoice #: " + invoiceNumber, 550, 800);
        drawTextRightAligned(content, font, 10, "Date: " + timestamp, 550, 785);
    }

    private void drawCustomerAndDealerInfo(PDPageContentStream content, PDType0Font regularFont, PDType0Font boldFont, String customerName, Dealer dealer) throws IOException {
        content.setStrokingColor(Color.LIGHT_GRAY);
        content.setLineWidth(1);
        content.moveTo(50, 750);
        content.lineTo(550, 750);
        content.stroke();

        content.setFont(boldFont, 11);
        drawText(content, "BILLED TO", 50, 725);
        drawText(content, "FROM", 350, 725);

        content.setFont(regularFont, 10);
        drawText(content, customerName, 50, 710);

        drawText(content, dealer.name(), 350, 710);
        drawText(content, dealer.address(), 350, 695);
        drawText(content, "Phone: " + dealer.phone(), 350, 680);
    }

    private void drawInvoiceTable(PDPageContentStream content, PDType0Font regularFont, PDType0Font boldFont, Vehicle vehicle, BigDecimal price, BigDecimal tax, BigDecimal total, NumberFormat formatter) throws IOException {
        float tableY = 620;
        float tableWidth = 500;
        float margin = 50;
        float rowHeight = 25;

        float descriptionX = margin + 10;
        float priceX = margin + 350;
        float totalX = margin + 490;

        content.setNonStrokingColor(new Color(240, 240, 240));
        content.addRect(margin, tableY, tableWidth, rowHeight);
        content.fill();

        content.setNonStrokingColor(Color.BLACK);
        content.setFont(boldFont, 10);
        drawText(content, "Description", descriptionX, tableY + 8);
        drawTextRightAligned(content, boldFont, 10, "Price", priceX, tableY + 8);
        drawTextRightAligned(content, boldFont, 10, "Total", totalX, tableY + 8);

        tableY -= rowHeight;

        content.setStrokingColor(Color.LIGHT_GRAY);
        content.addRect(margin, tableY, tableWidth, rowHeight);
        content.stroke();

        content.setFont(regularFont, 10);
        drawText(content, vehicle.make() + " " + vehicle.model(), descriptionX, tableY + 8);
        drawTextRightAligned(content, regularFont, 10, formatter.format(price), priceX, tableY + 8);
        drawTextRightAligned(content, regularFont, 10, formatter.format(price), totalX, tableY + 8);

        tableY -= 40;

        float summaryLabelX = margin + 300;

        drawText(content, "Subtotal:", summaryLabelX, tableY);
        drawTextRightAligned(content, regularFont, 10, formatter.format(price), totalX, tableY);

        tableY -= 20;

        drawText(content, "Tax (10%):", summaryLabelX, tableY);
        drawTextRightAligned(content, regularFont, 10, formatter.format(tax), totalX, tableY);

        tableY -= 15;
        content.setStrokingColor(Color.GRAY);
        content.setLineWidth(1);
        content.moveTo(summaryLabelX, tableY);
        content.lineTo(totalX, tableY);
        content.stroke();

        tableY -= 20;

        content.setFont(boldFont, 12);
        drawText(content, "Total Amount:", summaryLabelX - 40, tableY);
        drawTextRightAligned(content, boldFont, 12, formatter.format(total), totalX, tableY);
    }

    private void drawFooter(PDPageContentStream content, PDType0Font font) throws IOException {
        content.setFont(font, 10);
        drawText(content, "Thank you for your business!", 230, 100);
    }

    private void drawQrCode(PDDocument document, PDPage page, String invoiceNumber) throws WriterException, IOException {
        BufferedImage qrImage = generateQRCode(invoiceNumber);
        if (qrImage != null) {
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, qrImage);
            try (PDPageContentStream imageContent = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                imageContent.drawImage(pdImage, 50, 80, 100, 100);
            }
        }
    }

    private void drawText(PDPageContentStream content, String text, float x, float y) throws IOException {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private void drawTextRightAligned(PDPageContentStream content, PDType0Font font, float fontSize, String text, float rightEdgeX, float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float startX = rightEdgeX - textWidth;
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(startX, y);
        content.showText(text);
        content.endText();
    }

    private BufferedImage generateQRCode(String text) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
