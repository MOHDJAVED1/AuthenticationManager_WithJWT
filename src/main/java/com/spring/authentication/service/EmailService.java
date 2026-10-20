package com.spring.authentication.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.spring.authentication.entity.EmailTemplate;
import com.spring.authentication.repository.EmailTemplateRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

	private final JavaMailSender javaMailSender;
	private final EmailTemplateRepository emailTemplateRepository;
	private final String senderEmail;
	
	public EmailService(JavaMailSender javaMailSender, Environment environment, EmailTemplateRepository emailTemplateRepository) {
		this.javaMailSender = javaMailSender;
		this.emailTemplateRepository = emailTemplateRepository;
		this.senderEmail = environment.getRequiredProperty("spring.mail.username");
	}
	
	private void senderEmail(String to, String subject, String body) throws MessagingException {

		// Sending simple email
//		SimpleMailMessage message = new SimpleMailMessage();
//		message.setFrom(senderEmail);
//		message.setTo(to);
//		message.setSubject(subject);
//		message.setText(body);
		
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
		
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(body, true);
		
		javaMailSender.send(mimeMessage);
	}

	
	public void sendEmail(String templateCode, String recipientEmail, Map<String, Object> variables) {
	    
	    // 1. Fetch template from DB (Your current code)
	    EmailTemplate emailDetails = emailTemplateRepository.findByTemplateCode(templateCode)
	            .orElseThrow(() -> new RuntimeException("Template not found."));
	    
	    String subject = emailDetails.getSubject();
	    String bodyHtml = emailDetails.getBodyHtml();
	    
	    // 2. Convert variables map values to String safely
	    Map<String, String> stringVariables = variables.entrySet().stream()
	            .collect(Collectors.toMap(
	                    Map.Entry::getKey,
	                    e -> e.getValue() != null ? e.getValue().toString() : ""
	            ));

	    // 3. Replace {{placeholders}} with actual dynamic values
	    StringSubstitutor substitutor = new StringSubstitutor(stringVariables, "{{", "}}");
	    String resolvedSubject = substitutor.replace(subject);
	    String resolvedBody = substitutor.replace(bodyHtml);
	    
	    // 4. Send the email using JavaMailSender (example)
	    // mailSender.send(recipientEmail, resolvedSubject, resolvedBody);
	    
	    System.out.println("Email ready to send to: " + recipientEmail);
	    System.out.println("Subject: " + resolvedSubject);
	    System.out.println("emailBody : " + resolvedBody);
	    
	    try {
	    	senderEmail(recipientEmail, resolvedSubject, resolvedBody);
		} catch (MessagingException e1) {
			System.out.println("Email send failed due to : " + e1.getMessage());
		}
	}
}
