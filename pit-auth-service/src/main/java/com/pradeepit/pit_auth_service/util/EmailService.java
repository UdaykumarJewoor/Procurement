package com.pradeepit.pit_auth_service.util;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private Configuration freemarkerConfig;

    /**
     * Method to send email with FreeMarker template
     */
    public void sendEmail(String templateName, String to, String subject, Map<String, Object> templateData) {
        try {
            // Create a MimeMessage instance
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = getMimeMessageHelper(to, subject, mimeMessage);

            // Use FreeMarker template to generate the email body
            Template template = freemarkerConfig.getTemplate(templateName);
            StringWriter stringWriter = new StringWriter();
            template.process(templateData, stringWriter);

            // Set the body of the email as HTML
            messageHelper.setText(stringWriter.toString(), true);

            // Send the email using JavaMailSender
            javaMailSender.send(mimeMessage);
        } catch (MessagingException | TemplateException | IOException ignored) {

        }
    }


    private static MimeMessageHelper getMimeMessageHelper(String to, String subject, MimeMessage mimeMessage) throws MessagingException, UnsupportedEncodingException {
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);

        // Set the recipient email
        messageHelper.setTo(to);
        // Set the subject of the email
        messageHelper.setSubject(subject);

        // Sender name and email setup
        String senderName = "PradeepIT Testing TAP";  // Sender Name
        String senderEmail = "sachinkumarn9@gmail.com";       // Sender Email Address
        messageHelper.setFrom(senderEmail, senderName); // Set the From field with both name and email
        return messageHelper;
    }
}
