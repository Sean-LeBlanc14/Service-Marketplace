package com.ServiceMarketplace.service_marketplace.service;

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

    private final JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    @Value("${custom.app.sender.email}")
    private String SENDER_EMAIL;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public String generateEmailHtml(String code) {
        Context context = new Context();
        context.setVariable("code", code); 
        return templateEngine.process("verificationEmail", context);
    }
    
    public void sendVerificationEmail(String email, String code){

        MimeMessage message = mailSender.createMimeMessage();

        String html = generateEmailHtml(code);

        try{
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(SENDER_EMAIL);
            helper.setText(html, true);
            helper.setTo(email);
            helper.setSubject("Verification Code");
            mailSender.send(message);
        }catch(MessagingException e){
            throw new EmailSendException(email);
        }

    }

}
