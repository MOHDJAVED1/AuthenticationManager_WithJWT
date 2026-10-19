package com.spring.authentication.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.core.env.Environment;

@Service
public class EmailService {

	private final JavaMailSender javaMailSender;
	private final String senderEmail;
	
	public EmailService(JavaMailSender javaMailSender, Environment environment) {
		this.javaMailSender = javaMailSender;
		this.senderEmail = environment.getRequiredProperty("spring.mail.username");
	}
	
	public void sendEmail(String to, String subject, String body) throws MessagingException {

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
}
