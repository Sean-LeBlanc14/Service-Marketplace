package com.ServiceMarketplace.service_marketplace.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private TemplateEngine templateEngine;

    public String generateEmailHtml(String code) {
        Context context = new Context();
        context.setVariable("code", code); 
        return templateEngine.process("verificationEmail", context);
    }
    
    public void sendVerificationEmail(String email){

        String verificationCode = generateVerificationCode();

        verificationService.createVerification(email, verificationCode);

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

        String html = generateEmailHtml(verificationCode);

        try{
            helper.setFrom("servicemarket98@gmail.com");
            helper.setText(html, true);
            helper.setTo(email);
            helper.setSubject("Verification Code");
        }catch(MessagingException e){
            System.out.println("ERROR: Failed to create the message: " + e.getMessage());
        }

        mailSender.send(message);

    }

    private String generateVerificationCode(){
        SecureRandom secureRandom = new SecureRandom();

        int randomNumber = secureRandom.nextInt(100000);
        
        return String.format("%06d", randomNumber);
    }
}
