package com.spring.authentication.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

@Service
public class EmailService {

	private final JavaMailSender javaMailSender;
	private final String senderEmail;
	
	public EmailService(JavaMailSender javaMailSender, Environment environment) {
		this.javaMailSender = javaMailSender;
		this.senderEmail = environment.getRequiredProperty("spring.mail.username");
	}
	
	public void sendEmail(String to, String subject, String body) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(senderEmail);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(body);
		
		javaMailSender.send(message);
	}
}
