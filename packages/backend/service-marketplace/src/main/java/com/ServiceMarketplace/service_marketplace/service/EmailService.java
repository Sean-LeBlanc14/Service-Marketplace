package com.ServiceMarketplace.service_marketplace.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private TemplateEngine templateEngine;

    @Value("${custom.app.sender.email}")
    private String SENDER_EMAIL;


    public String generateEmailHtml(String code) {
        Context context = new Context();
        context.setVariable("code", code); 
        return templateEngine.process("verificationEmail", context);
    }
    
    public void sendVerificationEmail(String email, String code){

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

        String html = generateEmailHtml(code);

        try{
            helper.setFrom(SENDER_EMAIL);
            helper.setText(html, true);
            helper.setTo(email);
            helper.setSubject("Verification Code");
            mailSender.send(message);
        }catch(MessagingException e){
            
            System.out.println("ERROR: Failed to create the message: " + e.getMessage());
        }

    }

}
