package stockox_subscription_service.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.ILineDrawer;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import stockox_subscription_service.entity.PaymentTransaction;
import stockox_subscription_service.enums.PaymentStatus;
import stockox_subscription_service.exception.BadRequestException;
import stockox_subscription_service.exception.ResourceNotFoundException;
import stockox_subscription_service.repository.PaymentTransactionRepository;
import stockox_subscription_service.service.BillingInvoiceService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingInvoiceServiceImpl implements BillingInvoiceService {

    private final PaymentTransactionRepository transactionRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Brand Colors
    private static final DeviceRgb BRAND_PRIMARY = new DeviceRgb(67, 56, 202); // Indigo
    private static final DeviceRgb BRAND_LIGHT = new DeviceRgb(238, 242, 255); // Light Indigo
    private static final DeviceRgb SUCCESS_GREEN = new DeviceRgb(16, 185, 129);
    private static final DeviceRgb TEXT_DARK = new DeviceRgb(17, 24, 39);
    private static final DeviceRgb TEXT_MUTED = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(229, 231, 235);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @Override
    @Async
    public void sendInvoiceEmail(PaymentTransaction txn, String tenantEmail, String tenantName) {
        try {
            byte[] pdf = buildPdf(txn, tenantName);
            dispatchEmail(tenantEmail, tenantName, txn, pdf);
            log.info("Invoice email sent successfully to: {}", tenantEmail);
        } catch (Exception e) {
            log.error("Failed to send invoice email to: {}", tenantEmail, e);
        }
    }

    @Transactional
    @Override
    public byte[] generateInvoicePdf(UUID transactionId, UUID tenantId) {
        PaymentTransaction txn = resolveAndValidate(transactionId, tenantId);
        return buildPdf(txn, "Valued Customer");
    }

    @Override
    @Async
    public void resendInvoiceEmail(UUID transactionId, UUID tenantId) {
        PaymentTransaction txn = resolveAndValidate(transactionId, tenantId);
        if (txn.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Cannot resend invoices for unsuccessful payments.");
        }
        sendInvoiceEmail(txn, "stockoxindia@zohomail.in", "StockOx Customer");
    }

    private byte[] buildPdf(PaymentTransaction txn, String tenantName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(50, 50, 50, 50);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont lightFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Header Section with Logo and Invoice Title
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setMarginBottom(20);

            // Left side - Company Info
            Cell companyCell = new Cell().setBorder(Border.NO_BORDER);
            companyCell.add(new Paragraph("STOCKOX").setFont(boldFont).setFontSize(24).setFontColor(BRAND_PRIMARY));
            companyCell.add(new Paragraph("Financial Technology Platform").setFont(lightFont).setFontSize(10).setFontColor(TEXT_MUTED));
            companyCell.add(new Paragraph("GST: 29AAKCS1234P1ZR").setFont(lightFont).setFontSize(9).setFontColor(TEXT_MUTED));
            headerTable.addCell(companyCell);

            // Right side - Invoice Title
            Cell invoiceTitleCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            invoiceTitleCell.add(new Paragraph("TAX INVOICE").setFont(boldFont).setFontSize(18).setFontColor(BRAND_PRIMARY));
            invoiceTitleCell.add(new Paragraph("----------------------------------------").setFont(lightFont).setFontSize(8).setFontColor(BORDER_COLOR));
            invoiceTitleCell.add(new Paragraph("Invoice #: " + txn.getId().toString().substring(0, 8).toUpperCase())
                    .setFont(boldFont).setFontSize(11).setFontColor(TEXT_DARK));
            headerTable.addCell(invoiceTitleCell);
            doc.add(headerTable);

            // Divider
            doc.add(new LineSeparator((ILineDrawer) new SolidBorder(BORDER_COLOR, 1)));
            doc.add(new Paragraph(" ").setMarginTop(5).setMarginBottom(5));

            // Billing and Payment Details Section
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
            detailsTable.setWidth(UnitValue.createPercentValue(100));
            detailsTable.setMarginBottom(15);

            // Billed To
            Cell billedCell = new Cell().setBorder(Border.NO_BORDER);
            billedCell.add(new Paragraph("BILLED TO").setFont(boldFont).setFontSize(10).setFontColor(TEXT_MUTED));
            billedCell.add(new Paragraph(tenantName).setFont(boldFont).setFontSize(12).setFontColor(TEXT_DARK));
            billedCell.add(new Paragraph("Customer ID: " + txn.getTenantId().toString().substring(0, 12) + "...")
                    .setFont(lightFont).setFontSize(9).setFontColor(TEXT_MUTED));
            detailsTable.addCell(billedCell);

            // Invoice Details
            Cell invoiceDetailsCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            invoiceDetailsCell.add(new Paragraph("INVOICE DETAILS").setFont(boldFont).setFontSize(10).setFontColor(TEXT_MUTED));
            invoiceDetailsCell.add(new Paragraph("Invoice Date: " + txn.getCreatedAt().format(DATE_FMT))
                    .setFont(regularFont).setFontSize(10).setFontColor(TEXT_DARK));
            if (txn.getPaidAt() != null) {
                invoiceDetailsCell.add(new Paragraph("Payment Date: " + txn.getPaidAt().format(DATE_TIME_FMT))
                        .setFont(regularFont).setFontSize(10).setFontColor(TEXT_DARK));
            }
            invoiceDetailsCell.add(new Paragraph("Payment Method: " + getPaymentMethod(txn))
                    .setFont(regularFont).setFontSize(10).setFontColor(TEXT_DARK));
            invoiceDetailsCell.add(new Paragraph("Transaction ID: " + txn.getRazorpayPaymentId())
                    .setFont(lightFont).setFontSize(9).setFontColor(TEXT_MUTED));
            detailsTable.addCell(invoiceDetailsCell);
            doc.add(detailsTable);

            doc.add(new Paragraph(" ").setMarginTop(5));

            // Items Table
            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{60, 20, 20}));
            itemsTable.setWidth(UnitValue.createPercentValue(100));
            itemsTable.setMarginTop(10);
            itemsTable.setMarginBottom(15);

            // Table Headers
            itemsTable.addCell(createHeaderCell("Description", boldFont));
            itemsTable.addCell(createHeaderCell("Amount", boldFont).setTextAlignment(TextAlignment.RIGHT));
            itemsTable.addCell(createHeaderCell("Total", boldFont).setTextAlignment(TextAlignment.RIGHT));

            // Table Data
            String planDisplayName = txn.getPlan() != null ? txn.getPlan().getDisplayName() : "Subscription Plan";
            itemsTable.addCell(createDataCell(planDisplayName + " - " + getBillingPeriod(txn), regularFont));
            itemsTable.addCell(createDataCell("₹ " + fmt(txn.getBaseAmount()), regularFont).setTextAlignment(TextAlignment.RIGHT));
            itemsTable.addCell(createDataCell("₹ " + fmt(txn.getBaseAmount()), regularFont).setTextAlignment(TextAlignment.RIGHT));

            doc.add(itemsTable);

            // Totals Section
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}));
            totalsTable.setWidth(UnitValue.createPercentValue(100));
            totalsTable.setMarginTop(10);

            // Left side empty
            totalsTable.addCell(new Cell().setBorder(Border.NO_BORDER));

            // Right side totals
            Cell totalsCell = new Cell().setBorder(Border.NO_BORDER);

            // Subtotal
            totalsCell.add(new Paragraph("Subtotal:").setFont(regularFont).setFontSize(10).setFontColor(TEXT_MUTED));
            totalsCell.add(new Paragraph("+ GST (" + getGstRate() + "%):").setFont(regularFont).setFontSize(10).setFontColor(TEXT_MUTED));
            totalsCell.add(new Paragraph(" ").setMarginTop(5));
            totalsCell.add(new Paragraph("Total Amount:").setFont(boldFont).setFontSize(12).setFontColor(TEXT_DARK));

            totalsTable.addCell(totalsCell);

            // Amounts right aligned
            Cell amountsCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            amountsCell.add(new Paragraph("₹ " + fmt(txn.getBaseAmount())).setFont(regularFont).setFontSize(10).setFontColor(TEXT_MUTED));
            amountsCell.add(new Paragraph("₹ " + fmt(txn.getGstAmount())).setFont(regularFont).setFontSize(10).setFontColor(TEXT_MUTED));
            amountsCell.add(new Paragraph(" ").setMarginTop(5));
            amountsCell.add(new Paragraph("₹ " + fmt(txn.getTotalAmount())).setFont(boldFont).setFontSize(14).setFontColor(SUCCESS_GREEN));

            totalsTable.addCell(amountsCell);
            doc.add(totalsTable);

            // Payment Status Badge
            Table statusTable = new Table(UnitValue.createPercentArray(new float[]{100}));
            statusTable.setWidth(UnitValue.createPercentValue(100));
            statusTable.setMarginTop(20);
            statusTable.setMarginBottom(20);

            Cell statusCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
            statusCell.setBackgroundColor(BRAND_LIGHT);
            statusCell.setPadding(10);
            statusCell.add(new Paragraph("✓ PAYMENT CONFIRMED")
                    .setFont(boldFont).setFontSize(12).setFontColor(SUCCESS_GREEN).setTextAlignment(TextAlignment.CENTER));
            statusTable.addCell(statusCell);
            doc.add(statusTable);

            // Footer Section
            doc.add(new LineSeparator((ILineDrawer) new SolidBorder(BORDER_COLOR, 1)));
            doc.add(new Paragraph(" ").setMarginTop(10));

            Table footerTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
            footerTable.setWidth(UnitValue.createPercentValue(100));

            // Support Info
            Cell supportCell = new Cell().setBorder(Border.NO_BORDER);
            supportCell.add(new Paragraph("For support, contact:").setFont(lightFont).setFontSize(9).setFontColor(TEXT_MUTED));
            supportCell.add(new Paragraph("Email: support@stockox.com").setFont(lightFont).setFontSize(9).setFontColor(TEXT_MUTED));
            supportCell.add(new Paragraph("Phone: +91 80 1234 5678").setFont(lightFont).setFontSize(9).setFontColor(TEXT_MUTED));
            footerTable.addCell(supportCell);

            // Terms
            Cell termsCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            termsCell.add(new Paragraph("This is a system generated invoice").setFont(lightFont).setFontSize(8).setFontColor(TEXT_MUTED));
            termsCell.add(new Paragraph("Valid for GST purposes").setFont(lightFont).setFontSize(8).setFontColor(TEXT_MUTED));
            footerTable.addCell(termsCell);

            doc.add(footerTable);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate invoice PDF for transaction: {}", txn.getId(), e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private void dispatchEmail(String to, String tenantName, PaymentTransaction txn, byte[] pdfBytes) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Your StockOX Invoice - " + txn.getId().toString().substring(0, 8).toUpperCase());

        String emailBody = buildEmailBody(tenantName, txn);
        helper.setText(emailBody, true);
        helper.addAttachment("StockOX_Invoice_" + txn.getId().toString().substring(0, 8) + ".pdf",
                () -> new ByteArrayInputStream(pdfBytes));

        mailSender.send(message);
        log.info("Invoice email dispatched to: {}", to);
    }

    private String buildEmailBody(String tenantName, PaymentTransaction txn) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #4338CA;">Thank you for your payment, %s!</h2>
                    <p>Your payment of <strong>₹ %s</strong> has been successfully processed.</p>
                    <p>Please find attached your tax invoice for the %s plan.</p>
                    <div style="background-color: #F3F4F6; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p><strong>Transaction ID:</strong> %s</p>
                        <p><strong>Payment ID:</strong> %s</p>
                        <p><strong>Date:</strong> %s</p>
                    </div>
                    <p>You can access your subscription details anytime from your dashboard.</p>
                    <hr style="border: none; border-top: 1px solid #E5E7EB; margin: 20px 0;">
                    <p style="color: #6B7280; font-size: 12px;">
                        This is an automated message. Please do not reply directly to this email.<br>
                        For support, contact us at support@stockox.com
                    </p>
                </div>
            </body>
            </html>
            """,
                tenantName,
                fmt(txn.getTotalAmount()),
                txn.getPlan() != null ? txn.getPlan().getDisplayName() : "Subscription",
                txn.getId().toString().substring(0, 8).toUpperCase(),
                txn.getRazorpayPaymentId() != null ? txn.getRazorpayPaymentId() : "N/A",
                txn.getPaidAt() != null ? txn.getPaidAt().format(DATE_TIME_FMT) : txn.getCreatedAt().format(DATE_FMT)
        );
    }

    private PaymentTransaction resolveAndValidate(UUID transactionId, UUID tenantId) {
        PaymentTransaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!txn.getTenantId().equals(tenantId)) {
            throw new BadRequestException("Unauthorized access to invoice");
        }
        if (txn.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Invoice only available for successful payments");
        }
        return txn;
    }

    private Cell createHeaderCell(String text, PdfFont font) {
        return new Cell()
                .setBackgroundColor(BRAND_PRIMARY)
                .setBorder(Border.NO_BORDER)
                .setPadding(10)
                .add(new Paragraph(text).setFont(font).setFontSize(11).setFontColor(ColorConstants.WHITE));
    }

    private Cell createDataCell(String text, PdfFont font) {
        return new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setPadding(8)
                .add(new Paragraph(text).setFont(font).setFontSize(10).setFontColor(TEXT_DARK));
    }

    private String getPaymentMethod(PaymentTransaction txn) {
        // You can enhance this based on your payment gateway response
        if (txn.getRazorpayPaymentId() != null) {
            return "Razorpay (Online Payment)";
        }
        return "Online Payment";
    }

    private String getBillingPeriod(PaymentTransaction txn) {
        if (txn.getBillingPeriodStart() != null && txn.getBillingPeriodEnd() != null) {
            return txn.getBillingPeriodStart().format(DATE_FMT) + " to " + txn.getBillingPeriodEnd().format(DATE_FMT);
        }
        return "Monthly Subscription";
    }

    private String getGstRate() {
        // You can make this configurable
        return "18";
    }

    private static String fmt(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
}