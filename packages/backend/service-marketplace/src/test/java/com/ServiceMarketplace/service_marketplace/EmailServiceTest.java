package com.ServiceMarketplace.service_marketplace;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.ServiceMarketplace.service_marketplace.exception.EmailSendException;
import com.ServiceMarketplace.service_marketplace.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, templateEngine);
        ReflectionTestUtils.setField(emailService, "SENDER_EMAIL", "noreply@polyservices.test");
        lenient().when(mailSender.createMimeMessage())
            .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        lenient().when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>ok</html>");
    }

    @Test
    void sendVerificationEmail_rendersTemplateAndSendsMessage() {
        emailService.sendVerificationEmail("student@calpoly.edu", "123456");

        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verificationEmail"), context.capture());
        assertThat(context.getValue().getVariable("code")).isEqualTo("123456");
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProviderBookingNotificationEmail_rendersTemplateAndSendsMessage() {
        Instant scheduledAt = Instant.parse("2026-06-04T18:00:00Z");

        emailService.sendProviderBookingNotificationEmail(
            "provider@calpoly.edu",
            "Bob",
            "Alice",
            "Math Tutoring",
            new BigDecimal("50"),
            null,
            scheduledAt,
            "https://example.test/confirm",
            "https://example.test/cancel"
        );

        verifyTemplateRenderedAndMessageSent("providerBookingNotification");
    }

    @Test
    void sendBookingRequestedCustomerEmail_rendersTemplateAndSendsMessage() {
        Instant scheduledAt = Instant.parse("2026-06-04T18:00:00Z");

        emailService.sendBookingRequestedCustomerEmail(
            "student@calpoly.edu",
            "Alice",
            "Math Tutoring",
            new BigDecimal("50"),
            "per hour",
            scheduledAt,
            "booking-1"
        );

        verifyTemplateRenderedAndMessageSent("bookingRequestedCustomer");
    }

    @Test
    void sendBookingConfirmedCustomerEmail_rendersTemplateAndSendsMessage() {
        Instant scheduledAt = Instant.parse("2026-06-04T18:00:00Z");

        emailService.sendBookingConfirmedCustomerEmail(
            "student@calpoly.edu",
            "Alice",
            "Math Tutoring",
            new BigDecimal("50"),
            "per hour",
            scheduledAt,
            "booking-1"
        );

        verifyTemplateRenderedAndMessageSent("bookingConfirmedCustomer");
    }

    @Test
    void sendBookingConfirmedProviderEmail_rendersTemplateAndSendsMessage() {
        Instant scheduledAt = Instant.parse("2026-06-04T18:00:00Z");

        emailService.sendBookingConfirmedProviderEmail(
            "provider@calpoly.edu",
            "Bob",
            "Math Tutoring",
            new BigDecimal("50"),
            "per hour",
            scheduledAt,
            "booking-1"
        );

        verifyTemplateRenderedAndMessageSent("bookingConfirmedProvider");
    }

    @Test
    void sendBookingCancelledProviderEmail_rendersTemplateAndSendsMessage() {
        Instant scheduledAt = Instant.parse("2026-06-04T18:00:00Z");

        emailService.sendBookingCancelledProviderEmail(
            "provider@calpoly.edu",
            "Bob",
            "Alice",
            "Math Tutoring",
            scheduledAt,
            "booking-1"
        );

        verifyTemplateRenderedAndMessageSent("bookingCancelledProvider");
    }

    @Test
    void sendBookingRejectedProviderEmail_rendersTemplateAndSendsMessage() {
        Instant scheduledAt = Instant.parse("2026-06-04T18:00:00Z");

        emailService.sendBookingRejectedProviderEmail(
            "provider@calpoly.edu",
            "Bob",
            "Alice",
            "Math Tutoring",
            scheduledAt,
            "booking-1"
        );

        verifyTemplateRenderedAndMessageSent("bookingRejectedProvider");
    }

    @Test
    void sendBookingCancelledCustomerEmail_rendersTemplateAndSendsMessage() {
        Instant scheduledAt = Instant.parse("2026-06-04T18:00:00Z");

        emailService.sendBookingCancelledCustomerEmail(
            "student@calpoly.edu",
            "Alice",
            "Bob",
            "Math Tutoring",
            scheduledAt,
            "booking-1"
        );

        verifyTemplateRenderedAndMessageSent("bookingCancelledCustomer");
    }

    @Test
    void sendBookingRejectedCustomerEmail_rendersTemplateAndSendsMessage() {
        Instant scheduledAt = Instant.parse("2026-06-04T18:00:00Z");

        emailService.sendBookingRejectedCustomerEmail(
            "student@calpoly.edu",
            "Alice",
            "Bob",
            "Math Tutoring",
            scheduledAt,
            "booking-1"
        );

        verifyTemplateRenderedAndMessageSent("bookingRejectedCustomer");
    }

    @Test
    void sendVerificationEmail_messagingFailure_throwsEmailSendException() {
        try (MockedConstruction<MimeMessageHelper> ignored = Mockito.mockConstruction(
            MimeMessageHelper.class,
            (mock, context) -> doThrow(new MessagingException("bad from address"))
                .when(mock).setFrom(anyString())
        )) {
            assertThatThrownBy(() -> emailService.sendVerificationEmail("student@calpoly.edu", "123456"))
                .isInstanceOf(EmailSendException.class);
        }

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void generateEmailHtml_rendersVerificationTemplate() {
        when(templateEngine.process(eq("verificationEmail"), any(Context.class))).thenReturn("<p>123456</p>");

        String html = emailService.generateEmailHtml("123456");

        assertThat(html).isEqualTo("<p>123456</p>");
        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verificationEmail"), context.capture());
        assertThat(context.getValue().getVariable("code")).isEqualTo("123456");
    }

    private void verifyTemplateRenderedAndMessageSent(String templateName) {
        verify(templateEngine).process(eq(templateName), any(Context.class));
        verify(mailSender).send(any(MimeMessage.class));
    }
}
