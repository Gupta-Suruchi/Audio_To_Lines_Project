package com.audio_to_lines_project.audio_to_lines_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl {
    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String sender;
    MailStructure mailStructure = new MailStructure();

    public String sendEmail( String recipient, String TranscriptedText) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(sender);
        simpleMailMessage.setTo(recipient);
        simpleMailMessage.setSubject(mailStructure.getSubject());
        simpleMailMessage.setText(mailStructure.getBody()+ TranscriptedText);
        emailSender.send(simpleMailMessage);
        return "Email Sent Successfully...";
    }
}
