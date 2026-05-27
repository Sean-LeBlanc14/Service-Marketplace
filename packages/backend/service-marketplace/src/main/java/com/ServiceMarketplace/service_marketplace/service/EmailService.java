package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.ServiceMarketplace.service_marketplace.exception.EmailSendException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a z").withZone(ZoneId.of("UTC"));

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${custom.app.sender.email}")
    private String SENDER_EMAIL;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendVerificationEmail(String email, String code) {
        sendTemplatedEmail(email, "Verification Code", "verificationEmail",
            Map.of("code", code));
    }

    public void sendProviderBookingNotificationEmail(String toEmail, String providerName, String customerName,
            String serviceTitle, BigDecimal proposedPrice, String priceUnit, Instant scheduledAt) {
        sendTemplatedEmail(toEmail, "New Booking Request - " + serviceTitle, "providerBookingNotification",
            Map.of(
                "providerName", providerName,
                "customerName", customerName,
                "serviceTitle", serviceTitle,
                "proposedPrice", formatPrice(proposedPrice),
                "priceUnit", priceUnit != null ? priceUnit : "",
                "scheduledAt", DATE_FORMATTER.format(scheduledAt)
            ));
    }

    public void sendBookingConfirmedCustomerEmail(String toEmail, String customerName, String serviceTitle,
            BigDecimal agreedPrice, String priceUnit, Instant scheduledAt, String bookingId) {
        sendTemplatedEmail(toEmail, "Booking Confirmed - " + serviceTitle, "bookingConfirmedCustomer",
            Map.of(
                "customerName", customerName,
                "serviceTitle", serviceTitle,
                "agreedPrice", formatPrice(agreedPrice),
                "priceUnit", priceUnit != null ? priceUnit : "",
                "scheduledAt", DATE_FORMATTER.format(scheduledAt),
                "bookingId", bookingId
            ));
    }

    public void sendBookingConfirmedProviderEmail(String toEmail, String providerName, String serviceTitle,
            BigDecimal agreedPrice, String priceUnit, Instant scheduledAt, String bookingId) {
        sendTemplatedEmail(toEmail, "Booking Confirmed - Payment Received", "bookingConfirmedProvider",
            Map.of(
                "providerName", providerName,
                "serviceTitle", serviceTitle,
                "agreedPrice", formatPrice(agreedPrice),
                "priceUnit", priceUnit != null ? priceUnit : "",
                "scheduledAt", DATE_FORMATTER.format(scheduledAt),
                "bookingId", bookingId
            ));
    }

    private void sendTemplatedEmail(String toEmail, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        String html = templateEngine.process(templateName, context);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(SENDER_EMAIL);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendException(toEmail);
        }
    }

    private String formatPrice(BigDecimal price) {
        return "$" + price.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    // kept for backward compatibility with VerificationController resend path
    public String generateEmailHtml(String code) {
        Context context = new Context();
        context.setVariable("code", code);
        return templateEngine.process("verificationEmail", context);
    }
}
